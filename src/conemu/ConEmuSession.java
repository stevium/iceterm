package conemu;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import conemu.AnsiLog.AnsiStreamChunkReceivedListener;
import conemu.util.CommandLineBuilder;
import conemu.util.ProcessExitDetector;
import conemu.util.ProcessListener;
import conemu.util.WinApi;
import conemu.util.tasks.Task;
import conemu.util.tasks.TaskCompletionSource;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.apache.xerces.dom.DocumentImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sun.plugin.dom.exception.InvalidStateException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <p>A single session of the console emulator running a console process. Each console process execution in the control spawns a new console emulator and a new session.</p>
 * <p>When the console emulator starts, a console view appears in the control. The console process starts running in it immediately. When the console process terminates, the console emulator might or might not be closed, depending on the settings. After the console emulator closes, the control stops viewing the console, and this session ends.</p>
 */
public class ConEmuSession {

    private static final String DATEFORMAT = "yyyy'-'MM'-'dd'T'HH':'mm':'ss";

    private List<ConsoleEmulatorClosedListener> emulatorClosedEventListeners = new ArrayList<>();

    private List<ConsoleProcessExitedListener> processExitedEventListeners = new ArrayList<>();

    /*
     * A service option. Whether to load the ConEmu helper DLL in-process to communicate with ConEmu (<code>True</code>, new mode), or start a new helper process to send each command (<code>False</code>), legacy mode).
     */
    private static boolean isExecutingGuiMacroInProcess = true;

    /*
     * Non-NULL if we've requested ANSI log from ConEmu and are listening to it.
     */
    @Nullable
    private AnsiLog ansilog;

    /*
     * Per-session temp files, like the startup options for ConEmu and ANSI log cache.
     */
    @NotNull
    private File dirTempWorkingFolder;

    /*
     * Sends commands to the ConEmu instance and gets info from it.
     */
    @NotNull
    private GuiMacroExecutor guiMacroExecutor;

    /*
     * 	Executed to process disposal.
     */
    @NotNull
    private List<Consumer> lifetime = new ArrayList<>();

    /*
     * The exit code of the console process, if it has already exited. <code>Null</code>, if the console process is still running within the console emulator.
     */
    private Integer nConsoleProcessExitCode;

    /*
     * The ConEmu process, even after it exits.
     */
    @NotNull
    private Process process;

    // TODO
    /*
     * Stores the main thread scheduler, so that all state properties were only changed on this thread.
     */
    @NotNull
    private ScheduledExecutorService schedulerSta =
            Executors.newScheduledThreadPool(5);

    /*
     * The original parameters for this session; sealed, so they can't change after the session is run.
     */
    @NotNull
    private ConEmuStartInfo startinfo;

    /*
     * Task-based notification of the console emulator closing.
     */
    @NotNull
    private final TaskCompletionSource<ConsoleEmulatorClosedEvent> taskConsoleEmulatorClosed = new TaskCompletionSource<>();

    /*
     * Task-based notification of the console process exiting.
     */
    @NotNull
    private final TaskCompletionSource<ConsoleProcessExitedEvent> taskConsoleProcessExit = new TaskCompletionSource<>();

    /**
     * Starts the session.
     * Opens the emulator view in the control (HWND given in <code>hostContext</code> ) by starting the ConEmu child process and giving it that HWND; ConEmu then starts the child Console Process for the commandline given in <code>startInfo</code> and makes it run in the console emulator window.
     *
     * @param startInfo   User-defined startup parameters for the console process.
     * @param hostContext Control-related parameters.
     */
    public ConEmuSession(@NotNull ConEmuStartInfo startInfo, @NotNull HostContext hostContext) {
        if (startInfo == null)
            throw new NullArgumentException("startInfo");
        if (hostContext == null)
            throw new NullArgumentException("hostContext");
        if (startinfo.getConsoleProcessCommandLine() == null || StringUtils.isEmpty(startInfo.getConsoleProcessCommandLine()))
            throw new NullArgumentException("Cannot start a new console process for command line " + startInfo.getConsoleProcessCommandLine() + "because it's either NULL, or empty, or whitespace.");

        this.startinfo = startInfo;
        startInfo.MarkAsUsedUp(); // No more changes allowed in this copy

        // Directory for working files, +cleanup
        dirTempWorkingFolder = initTempWorkingFolder();

        // Events wiring: make sure sinks pre-installed with start-info also get notified
        initWireEvents(startInfo);

        // Should feed ANSI log?
        if (startInfo.isReadingAnsiStream())
            ansilog = initAnsiLog(startInfo);

        // Cmdline
        CommandLineBuilder commandLine = initMakeConEmuCommandLine(startInfo, hostContext, ansilog, dirTempWorkingFolder);

        // Start ConEmu
        // If it fails, lifetime will be terminated; from then on, termination will be bound to ConEmu process exit
        process = initStartConEmu(startInfo, commandLine);

        // GuiMacro executor
        guiMacroExecutor = new GuiMacroExecutor(startInfo.getConEmuConsoleServerExecutablePath());
        lifetime.add(o -> guiMacroExecutor.close());

        // Monitor payload process
        initConsoleProcessMonitoring();
    }

    /**
     * <p>Gets whether the console process has already exited (see {@link ConsoleProcessExitedEvent}). The console emulator view might have closed as well, but might have not (see {@link ConEmuStartInfo#getWhenConsoleProcessExits()}).</p>
     * <p>This state only changes on the main thread.</p>
     */
    public boolean isConsoleProcessExited() {
        return nConsoleProcessExitCode != null;
    }

    /**
     * <p>Gets the start info with which this session has been started.</p>
     * <p>All of the properties in this object are now readonly.</p>
     */
    @NotNull
    public ConEmuStartInfo getStartInfo() {
        return startinfo;
    }

    /**
     * Starts construction of the ConEmu GUI Macro, see <a href="http://conemu.github.io/en/GuiMacro.html">http://conemu.github.io/en/GuiMacro.html</a> .
     */
    public GuiMacroBuilder beginGuiMacro(@NotNull String sMacroName) {
        if (sMacroName == null)
            throw new NullArgumentException("sMacroName");

        return new GuiMacroBuilder(this, sMacroName, Collections.emptyList());
    }

    /**
     * An alias for {@link #closeConsoleEmulator()}.
     */
    public void close() {
        closeConsoleEmulator();
    }

    /**
     * <p>Closes the console emulator window, and kills the console process if it's still running.</p>
     * <p>This also closes the running session, the control goes blank and ready for running a new session.</p>
     * <p>To just kill the console process, use {@link #killConsoleProcessAsync()}. If {@link ConEmuStartInfo#getWhenConsoleProcessExits()} allows, the console emulator window might stay open after that.</p>
     */
    public void closeConsoleEmulator() {
        try {
            if (process.isAlive())
                beginGuiMacro("Close")
                        .withParam(1 /*terminate active process*/)
                        .withParam(1 /*without confirmation*/)
                        .executeSync();
            if (process.isAlive())
                process.destroy();
        } catch (Exception e) {
            // Might be a race, so in between HasExited and Kill state could change, ignore possible errors here
        }
    }

    /**
     * <p>Executes a ConEmu GUI Macro on the active console, see <a href="http://conemu.github.io/en/GuiMacro.html">http://conemu.github.io/en/GuiMacro.html</a> .</p>
     * <p>This function takes for formatted text of a GUI Macro; to format parameters correctly, better use the {@link #beginGuiMacro(String)} and the macro builder.</p>
     *
     * @param macrotext The full macro command, see <a href="http://conemu.github.io/en/GuiMacro.html">http://conemu.github.io/en/GuiMacro.html</a> .
     */
    public Task<GuiMacroResult> ExecuteGuiMacroTextAsync(@NotNull String macrotext) {
        if (macrotext == null)
            throw new NullArgumentException("macrotext");

        Process processConEmu = process;
        if (processConEmu == null)
            throw new InvalidStateException("Cannot execute a macro because the console process is not running at the moment.");

        int pid = WinApi.Helpers.getProcessId(process);

        return isExecutingGuiMacroInProcess ? guiMacroExecutor.executeInProcessAsync(pid, macrotext) : guiMacroExecutor.executeViaExtenderProcessAsync(macrotext, pid, startinfo.getConEmuConsoleExtenderExecetablePath());
    }

    /**
     * <p>Executes a ConEmu GUI Macro on the active console, see <a href="http://conemu.github.io/en/GuiMacro.html">http://conemu.github.io/en/GuiMacro.html</a> , synchronously.</p>
     * <p>This function takes for formatted text of a GUI Macro; to format parameters correctly, better use the {@link #beginGuiMacro(String)} and the macro builder.</p>
     *
     * @param macrotext The full macro command, see <a href="http://conemu.github.io/en/GuiMacro.html">http://conemu.github.io/en/GuiMacro.html</a> .
     */
    public GuiMacroResult ExecuteGuiMacroTextSync(@NotNull String macrotext) {
        if (macrotext == null)
            throw new NullArgumentException("macrotext");

        Task<GuiMacroResult> task = ExecuteGuiMacroTextAsync(macrotext);

        try {
            task.waitForCompletion();
        } catch (InterruptedException e) {
            // Not interested
        }

        return task.getResult();
    }

    /**
     * <p>Gets the exit code of the console process, if {@link #isConsoleProcessExited() it has already exited}. Throws an exception if it has not.</p>
     * <p>This state only changes on the main thread.</p>
     *
     * @return
     */
    public Integer getConsoleProcessExitCode() {
        Integer nCode = nConsoleProcessExitCode;
        if (nCode == null)
            throw new InvalidStateException("The exit code is not available yet because the console process is still running.");
        return nCode;
    }

    /**
     * <p>Kills the console process running in the console emulator window, if it has not exited yet.</p>
     * <p>This does not necessarily kill the console emulator process which displays the console window, but it might also close if {@link ConEmuStartInfo#getWhenConsoleProcessExits()} says so.</p>
     *
     * @return Whether the process were killed (otherwise it has been terminated due to some other reason, e.g. exited on its own or killed by a third party).
     */
    @NotNull
    public Task<Boolean> killConsoleProcessAsync() {
        try {
            if (process.isAlive() && nConsoleProcessExitCode == null) {
                return GetInfoRoot.queryAsync(this).continueWith(task -> {
                    GetInfoRoot rootInfo = task.getResult();
                    if (rootInfo.pid == null)
                        return false; // Has already exited
                    try {
                        String cmd = "taskkill /F /PID " + rootInfo.pid;
                        Runtime.getRuntime().exec(cmd);
                        return true;
                    } catch (Exception ex) {
                        // Most likely, has already exited
                        return false;
                    }
                });
            }
        } catch (Exception ex) {
            // Might be a race, so in between HarExited and Kill state could change, ignore possible errors here
        }
        return Task.forResult(false);
    }

    /**
     * <p>Sends the <code>Control+Break</code> signal to the console process, which will most likely abort it.</c></p>
     * <p>Unlike {@link #killConsoleProcessAsync()}, this is a soft signal which might be processed by the console process for a graceful shutdown, or ignored altogether.</p>
     */
    public Task sendControlBreakAsync() {
        try {
            if (process.isAlive())
                return beginGuiMacro("Break").withParam(1 /* Ctrl+Break */).executeAsync();
        } catch (Exception ex) {
            // Might be a race, so in between HasExited and Kill state could change, ignore possible errors here
        }
        return Task.forResult(false);
    }

    /**
     * <p>Sends the <code>Control+C</code> signal to the payload console process, which will most likely abort it.</p>
     * <p>Unlike {@link #killConsoleProcessAsync()}, this is a soft signal which might be processed by the console process for a graceful shutdown, or ignored altogether.</p>
     */
    public Task SendControlCAsync() {
        try {
            if (process.isAlive())
                return beginGuiMacro("Break").withParam(0 /* Ctrl+C */).executeAsync();
        } catch (Exception ex) {
            // Might be a race, so in between HasExited and Kill state could change, ignore possible errors here
        }
        return Task.forResult(false);
    }

    /**
     * <p>Waits until the console emulator closes and the console emulator view gets hidden from the control, or completes immediately if it has already exited.</p>
     * <p>Note that the console process might have terminated long before this moment without closing the console emulator unless {@link WhenConsoleProcessExits#CloseConsoleEmulator} were selected in the startup options.</p>
     */
    @NotNull
    public Task waitForConsoleEmulatorCloseAsync() {
        return taskConsoleEmulatorClosed.getTask();
    }

    /**
     * <p>Waits for the console process running in the console emulator to terminate, or completes immediately if it has already terminated.</p>
     * <p>If not {@link WhenConsoleProcessExits#CloseConsoleEmulator}, the console emulator stays, otherwise it closes also, and the console emulator window is hidden from the control.</p>
     */
    @NotNull
    public Task<ConsoleProcessExitedEvent> waitForConsoleProcessExitAsync() {
        return taskConsoleProcessExit.getTask();
    }

    /**
     * <p>Writes text to the console input, as if it's been typed by user on the keyboard.</p>
     * <p>Whether this will be visible (=echoed) on screen is up to the running console process.</p>
     */
    public Task writeInputText(@NotNull String text) {
        if (text == null)
            throw new NullArgumentException("text");
        if (text.length() == 0)
            return Task.forResult(false);

        return beginGuiMacro("Paste").withParam(2).withParam(text).executeAsync();
    }

    /**
     * <p>Writes text to the console output, as if the current running console process has written it to stdout.</p>
     * <p>Use with caution, as this might interfere with console process output in an unpredictable manner.</p>
     */
    public Task writeOutputText(@NotNull String text) {
        if (text == null)
            throw new NullArgumentException("text");
        if (text.length() == 0)
            return Task.forResult(false);

        return beginGuiMacro("Write").withParam(text).executeAsync();
    }

    /**
     * <p>Adds the specified ansi stream chunk listener to receive events from this ansi log</p>
     * <p>Fires on the main thread when the console process writes into its output or error streams. Gets a chunk of the raw ANSI stream contents.</p>
     * <p>For processes which write immediately on startup, this event might fire some chunks before you can start sinking it. To get notified reliably, use {@link ConEmuStartInfo#getAnsiStreamChunkReceivedEventSink()}.</p>
     * <p>To enable sinking this event, you must have {@link ConEmuStartInfo#isReadingAnsiStream()} set to <code>True</code> before starting the console process.</c></p>
     * <p>If you're reading the ANSI log with {@link #addAnsiStreamChunkReceivedListener(AnsiStreamChunkReceivedListener)}, it's guaranteed that all the events for the log will be fired before {@link ConsoleProcessExitedEvent}", and there will be no events afterwards.</p>
     */
    public void addAnsiStreamChunkReceivedListener(AnsiStreamChunkReceivedListener value) {
        if (ansilog == null)
            throw new IllegalStateException("You cannot receive the ANSI stream data because the console process has not been set up to read the ANSI stream before running; set ConEmuStartInfo::IsReadingAnsiStream to True before starting ghe process.");
        ansilog.addAnsiStreamChunkEventListener(value);
    }

    /**
     * <p>Removes specified ansi stream chunk listener so that it no longer receives events from this ansi log</p>
     *
     * @see #addAnsiStreamChunkReceivedListener(AnsiStreamChunkReceivedListener)
     */
    public void removeAnsiStreamChunkReceivedHandler(AnsiStreamChunkReceivedListener value) {
        if (ansilog == null)
            throw new IllegalStateException("You cannot receive the ANSI stream data because the console process has not been set up to read the ANSI stream before running; set ConEmuStartInfo::IsReadingAnsiStream to True before starting ghe process.");
        ansilog.removeAnsiStreamChunkEventListener(value);
    }

    @NotNull
    private AnsiLog initAnsiLog(ConEmuStartInfo startinfo) {
        AnsiLog ansiLog = new AnsiLog(dirTempWorkingFolder);
        lifetime.add(o -> ansiLog.dispose());
        if (startinfo.getAnsiStreamChunkReceivedEventSink() != null)
            ansiLog.addAnsiStreamChunkEventListener(startinfo.getAnsiStreamChunkReceivedEventSink());

        // Do the pumping periodically (TODO: take this to async?.. but would like to keep the final evt on the home thread, unless we go to tasks)
        // TODO: if ConEmu writes to a pipe, we might be getting events when more data comes to the pipe rather than poll it by timer
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ansiLog.pumpStream();
            }
        }, 100, 100);
        lifetime.add(o -> timer.cancel());

        return ansiLog;
    }

    /**
     * Watches for the status of the payload console process to fetch its exitcode when done and notify user of that.
     */
    private void initConsoleProcessMonitoring() {
        // When the payload process exits, use its exit code
        Consumer<Task<Integer>> λExited = task -> {
            if (task.getResult() == null) // Means the wait were aborted, e.g. ConEmu has been shut down and we processed that on the main thread
                return;
            tryFireConsoleProcessExited(task.getResult());
        };

        // Detect when this happens
        initPayloadProcessMonitoring_WaitFoxExitCodeAsync().continueWith(task -> {
            λExited.accept(task);
            return null;
        }); // TODO use main thread executor
    }

    @NotNull
    private CommandLineBuilder initMakeConEmuCommandLine(ConEmuStartInfo startinfo, HostContext hostcontext, AnsiLog ansilog, File dirLocalTempRoot) {
        if (startinfo == null)
            throw new NullArgumentException("startinfo");
        if (hostcontext == null)
            throw new NullArgumentException("hostcontext");

        CommandLineBuilder cmdl = new CommandLineBuilder();

        // This sets up hosting of ConEmu in our control
        cmdl.appendSwitch("-InsideWnd");
        cmdl.appendFileNameIfNotNull("0x" + Long.toHexString(hostcontext.HWndParent.getPointer().getLong(0)));

        // Don't use keyboard hooks in ConEmu when embedded
        cmdl.appendSwitch("-NoKeyHooks");

        switch (startinfo.getLogLevel()) {
            case Basic:
                cmdl.appendSwitch("-Log");
                break;
            case Detailed:
                cmdl.appendSwitch("-Log2");
                break;
            case Andvanced:
                cmdl.appendSwitch("-Log3");
                break;
            case Full:
                cmdl.appendSwitch("-Log4");
                break;
        }

        // Basic settings, like fonts ahd hidden tab bar
        // Plus some of the properties on this class
        cmdl.appendSwitch("-LoadCfgFile");
        cmdl.appendFileNameIfNotNull(initMakeConEmuCommandLine_EmitConfigFile(dirLocalTempRoot, startinfo, hostcontext));

        if (startinfo.getStartupDirectory() == null || StringUtils.isEmpty(startinfo.getStartupDirectory())) {
            cmdl.appendSwitch("-Dir");
            cmdl.appendFileNameIfNotNull(startinfo.getStartupDirectory());
        }

        // ANSI Log file
        if (ansilog != null) {
            cmdl.appendSwitch("-AnsiLog");
            cmdl.appendFileNameIfNotNull(ansilog.getDirectory().getAbsolutePath());
        }
        if (dirLocalTempRoot == null)
            throw new NullArgumentException("dirLocalTempRoot");

        cmdl.appendSwitch("-cmd");

        // Console mode command
        // NOTE: if placed AFTER the payload command line, otherwise somehow ConEmu hooks won't fetch the switch out of the cmdline, e.g. with some complicated git fetch/push cmdline syntax which has a lot of colons inside on itself
        String sConsoleExitMode;
        switch (startinfo.getWhenConsoleProcessExits()) {
            case CloseConsoleEmulator:
                sConsoleExitMode = "n";
                break;
            case KeepConsoleEmulator:
                sConsoleExitMode = "c0";
                break;
            case KeepConsoleEmulatorAndShowMessage:
                sConsoleExitMode = "c";
            default:
                throw new IllegalArgumentException("ConEmuStartInfo::WhenConsoleProcessExits"
                        + startinfo.getWhenConsoleProcessExits() + "This is not a valid enum value.");
        }
        cmdl.appendSwitchIfNotNull("-cur_console:", (startinfo.isElevated() ? "a" : "") + sConsoleExitMode);

        // And the shell command line itself
        cmdl.appendSwitch(startinfo.getConsoleProcessCommandLine());

        return cmdl;
    }

    private String initMakeConEmuCommandLine_EmitConfigFile(@NotNull File dirForConflgFile, @NotNull ConEmuStartInfo startinfo, @NotNull HostContext hostcontext) {
        if (dirForConflgFile == null)
            throw new NullArgumentException("dirForConflgFile");
        if (startinfo == null)
            throw new NullArgumentException("startinfo");
        if (hostcontext == null)
            throw new NullArgumentException("hostcontext");

        // Take baseline settings from the startinfo
        Document xmlBase = startinfo.getBaseConfiguration();
        if (xmlBase.getDocumentElement() == null)
            throw new IllegalStateException("The BaseConfiguration parameter of the ConEmuSTartInfo must be a non-empty XmlDocument. This one does not have a root element.");
        if (xmlBase.getDocumentElement().getTagName() != ConEmuConstants.XmlElementKey)
            throw new IllegalStateException("The BaseConfiguration parameter of the ConEmuStartInfo must be an XmlDocument with the root element named " + ConEmuConstants.XmlElementKey + " in an empty namespace. The actual element name is " + xmlBase.getDocumentElement().getTagName() + ".");
        if (!xmlBase.getDocumentElement().getAttribute(ConEmuConstants.XmlAttrName).equalsIgnoreCase(ConEmuConstants.XmlValueSoftware))
            throw new IllegalStateException("The BaseConfiguration parameter of the ConEmuStartInfo must be an XmlDocument whose root element is named " + ConEmuConstants.XmlElementKey + " and has an attribute " + ConEmuConstants.XmlAttrName + " set to " + ConEmuConstants.XmlValueSoftware + ". The actual value of this attribute is " + xmlBase.getDocumentElement().getAttribute(ConEmuConstants.XmlAttrName) + ".");

        // Load default tmeplate
        Document xmlDoc = new DocumentImpl();
        xmlDoc.appendChild(xmlDoc.importNode(xmlBase.getDocumentElement(), true));

        // Ensure the settings file has the expected keys structure
        // As we now allow user-supplied documents, we must ensure these elements exist
        Element xmlSoftware = xmlDoc.getDocumentElement();
        if (xmlSoftware == null)
            throw new IllegalStateException("Not expecting the cloned element to bu NULL.");

        XPath xPath = XPathFactory.newInstance().newXPath();
        Element xmlConEmu = null;
        try {
            xmlConEmu = (Element) xPath.evaluate(ConEmuConstants.XmlElementKey + "[" + ConEmuConstants.XmlAttrName + "='" + ConEmuConstants.XmlValueConEmu + "']", xmlSoftware, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            // create new element
        }
        if (xmlConEmu == null) {
            xmlConEmu = xmlDoc.createElement(ConEmuConstants.XmlElementKey);
            xmlSoftware.appendChild(xmlConEmu);
            xmlConEmu.setAttribute(ConEmuConstants.XmlAttrName, ConEmuConstants.XmlValueConEmu);
        }

        Element xmlDotVanilla = null;
        try {
            xmlDotVanilla = (Element) xPath.evaluate(ConEmuConstants.XmlElementKey + "[" + ConEmuConstants.XmlAttrName + "='" + ConEmuConstants.XmlValueDotVanilla + "']", xmlSoftware, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            // create new element
        }
        if (xmlDotVanilla == null) {
            xmlDotVanilla = xmlDoc.createElement(ConEmuConstants.XmlElementKey);
            xmlConEmu.appendChild(xmlDotVanilla);
            xmlDotVanilla.setAttribute(ConEmuConstants.XmlAttrName, ConEmuConstants.XmlValueDotVanilla);
        }

        // Apply settings from properties
        Element xmlSettings = xmlDotVanilla;
        {
            String keyname = "StatusBar.Show";
            Element xmlElem = null;
            try {
                xmlElem = (Element) xPath.evaluate("value[@name='" + keyname + "']", xmlSettings, XPathConstants.NODE);
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
            if (xmlElem == null)
                xmlElem = (Element) xmlSettings.appendChild(xmlDoc.createElement("value"));
            xmlElem.setAttribute(ConEmuConstants.XmlAttrName, keyname);
            xmlElem.setAttribute("type", "hex");
            xmlElem.setAttribute("data", String.valueOf(hostcontext.IsStatusbarVisibleInitial ? 1
                    : 0));

        }

        //Enviroment variables
        if (startinfo.EnumEnv().iterator().hasNext() || startinfo.isEchoingConsoleCommandLine() || startinfo.getGreetingText().length() > 0) {
            String keyname = "EnvironmentSet";

            Element xmlElem = null;
            try {
                xmlElem = (Element) xPath.evaluate("value[@name='" + keyname + "']", xmlSettings, XPathConstants.NODE);
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
            if (xmlElem == null)
                xmlElem = (Element) xmlSettings.appendChild(xmlDoc.createElement("value"));
            xmlElem.setAttribute(ConEmuConstants.XmlAttrName, keyname);
            xmlElem.setAttribute("type", "multi");
            for (String key :
                    startinfo.EnumEnv()) {
                Element xmlLine = xmlDoc.createElement("line");
                xmlElem.appendChild(xmlLine);
                xmlLine.setAttribute("data", "set " + key + "=" + startinfo.GetEnv(key));
            }

            // Echo the custom greeting text
            if (startinfo.getGreetingText().length() > 0) {
                // Echo each line separately
                List<String> lines = Arrays.asList(startinfo.getGreetingText().split("\\r\\n|\\n|\\r"));
                if (lines != null && !lines.isEmpty() && lines.get(lines.size() - 1).length() == 0) // NewLine handling, as declared
                    lines.remove(lines.size() - 1);
                for (String line :
                        lines) {
                    Element xmlLine;
                    xmlLine = xmlDoc.createElement("line");
                    xmlElem.appendChild(xmlLine);
                    xmlLine.setAttribute("data", "echo " + initMakeConEmuCommandLine_EmitConfigFile_EscapeEchoText(line));
                }
            }

            // To echo the cmdline, add an echo command to the env-init session
            if (startinfo.isEchoingConsoleCommandLine()) {
                Element xmlLine;
                xmlLine = xmlDoc.createElement("line");
                xmlLine.setAttribute("data", "echo " + initMakeConEmuCommandLine_EmitConfigFile_EscapeEchoText(startinfo.getConsoleProcessCommandLine()));
            }
        }

        // Write out to temp location
        dirForConflgFile.mkdirs();
        String sConfigFile = Paths.get(dirForConflgFile.getAbsolutePath(), "Config.Xml").toString();

        try {
            Transformer transformer = null;
            transformer = TransformerFactory.newInstance().newTransformer();
            Result output = new StreamResult(new File(sConfigFile));
            Source input = new DOMSource(xmlDoc);
            transformer.transform(input, output); // Mark the file as readonly, so that ConEmu didn't suggest to save modifications to this temp file
            new File(sConfigFile).setReadOnly();
        } catch (Exception ex) {
        }

        return sConfigFile;
    }

    /**
     * Applies escaping so that (1) it went as a single argument into the ConEmu's <code>NextArg</code> function; (2) its special chars were escaped according to the ConEmu's <code>DoOutput</code> function which implements this echo.
     */
    private String initMakeConEmuCommandLine_EmitConfigFile_EscapeEchoText(String text) {
        if (text == null)
            throw new NullArgumentException("text");

        StringBuilder sb = new StringBuilder(text.length() + 2);

        // We'd always quote the arg; no harm, and works better with an empty string
        sb.append('"');

        for (char ch :
                text.toCharArray()) {
            switch (ch) {
                case '"':
                    sb.append('"').append('"'); // Quotes are doubled in this format
                    break;
                case '^':
                    sb.append("^^");
                    break;
                case '\r':
                    sb.append("^R");
                    break;
                case '\n':
                    sb.append("^N");
                case '\t':
                    sb.append("^T");
                    break;
                case '\u0007':
                    sb.append("^A");
                    break;
                case '\b':
                    sb.append("^B");
                    break;
                case '[':
                    sb.append("^E");
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }

        // Close arg quoting
        sb.append('"');

        return sb.toString();
    }

    /**
     * Async-loop retries for getting the root payload process to await its exit.
     */
    private Task<Integer> initPayloadProcessMonitoring_WaitFoxExitCodeAsync() {
        TaskCompletionSource<Integer> tasker = new TaskCompletionSource<>();
        Thread t = new Thread(() -> {
            for (; ; ) {
                // Might have been terminated on the main thread
                if (nConsoleProcessExitCode != null)
                    tasker.setError(null);
                if (!process.isAlive())
                    tasker.setError(null);

                // Ask ConEmu for PID
                try {
                    Task<Integer> getExitCode = GetInfoRoot.queryAsync(this).continueWith(task -> {
                        GetInfoRoot rootinfo = task.getResult();

                        // Check if the process has exited
                        if (rootinfo.exitCode != null)
                            return rootinfo.exitCode;

                        // If it has started already, must get a PID
                        // Await till the process exits and loop to re-ask conemu for its result
                        // If conemu exits too in this time, then it will republish payload exit code as its own exit code, and implementation will use it
                        if (rootinfo.pid != null) {
                            WinApi.Helpers.WaitForProcessExitAsync(rootinfo.pid).waitForCompletion();
                            return -1;
                        }
                        return null;
                    });
                    getExitCode.waitForCompletion();
                    // Do not wait before retrying
                    if (getExitCode.getResult() == -1) {
                        continue;
                    }
                    // Set valid result, and exit
                    if (getExitCode.getResult() != null) {
                        tasker.setResult(getExitCode.getResult());
                        return;
                    }
                } catch (InterruptedException e) {
                    // Smth failed, wait and retry
                }

                // Await before retrying once more
                try {
                    TimeUnit.MICROSECONDS.sleep(10);
                } catch (InterruptedException e) {

                }
            }
        });
        t.start();
        return tasker.getTask();
    }

    @NotNull
    private Process initStartConEmu(@NotNull ConEmuStartInfo startInfo, @NotNull CommandLineBuilder cmdl) {
        if (startInfo == null)
            throw new NullArgumentException("startInfo");
        if (cmdl == null)
            throw new NullArgumentException("cmdl");

        try {
            if (startInfo.getConEmuExecutablePath() == null || StringUtils.isEmpty(startInfo.getConEmuExecutablePath()))
                throw new IllegalStateException("Could not run the console emulator. The path to ConEmu.exe could not be detected.");
            if (!(new File(startInfo.getConEmuExecutablePath()).exists()))
                throw new IllegalStateException("Missing ConEmu executable at location " + startInfo.getConEmuExecutablePath() + ".");
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(startInfo.getConEmuExecutablePath(), cmdl.toString());

            // Bind process termination
            ProcessExitDetector processExitDetector = null;
            try {
                processExitDetector = new ProcessExitDetector(processBuilder.start());
                processExitDetector.addProcessListener(new ProcessListener() {
                    @Override
                    public void processFinished(Process process) {
                        terminateLifetime();
                        tryFireConsoleProcessExited(process.exitValue());
                        consoleEmulatorClosed(this, null);
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException("The process did not start.");
            }
            return process;
        } catch (Exception ex) {
            terminateLifetime();
            throw new IllegalStateException("Could not run the console emulator. " + ex.getMessage() + System.lineSeparator() + System.lineSeparator() + "Command:" + System.lineSeparator() + startInfo.getConEmuExecutablePath() + System.lineSeparator() + System.lineSeparator() + "Arguments:" + System.lineSeparator() + cmdl, ex);
        }
    }

    @NotNull
    private File initTempWorkingFolder() {
        final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(new Date());

        File dirTempWorkingDir = new File(Paths.get(System.getProperty("java.io.tmpdir"), "ConEmu", utcTime.replace(':', '-') + "." + String.format("%1$08X.%2$08X", Kernel32.INSTANCE.GetCurrentProcessId(), this.hashCode())).toString());// Prefixed with date-sortable then PID; then sync table id of this objec

        lifetime.add(o -> {
            try {
                if (dirTempWorkingFolder.exists())
                    dirTempWorkingFolder.delete();
            } catch (Exception ex) {
                // Not interested
            }
        });

        return dirTempWorkingDir;
    }

    private void initWireEvents(@NotNull ConEmuStartInfo startinfo) {
        if (startinfo == null)
            throw new NullArgumentException("startinfo");

        // Advise events before they got chance to fire, use event sinks from startinfo for guaranteed delivery
        if (startinfo.getConsoleProcessExitedEventSink() != null)
            addConsoleProcessExitedEventSink(startinfo.getConsoleProcessExitedEventSink());
        if (startinfo.getConsoleEmulatorClosedEventSink() != null)
            addConsoleEmulatorClosedEventSink(startinfo.getConsoleEmulatorClosedEventSink());

        // Re-issue events as async tasks
        // As we advise events before they even fire, the task is guaranteed to get its state
        addConsoleProcessExitedEventSink((source, event) -> taskConsoleProcessExit.setResult(event));
        addConsoleEmulatorClosedEventSink((source, event) -> taskConsoleEmulatorClosed.setResult(event));

    }

    private void terminateLifetime() {
        List<Consumer> items = lifetime;
        ListIterator<Consumer> li = items.listIterator(items.size());
        lifetime.clear();
        while (li.hasPrevious()) {
            li.previous().accept(null);
        }
    }

    /**
     * Fires the payload exited event if it has not been fired yet.
     *
     * @param nConsoleProcessExitCode
     */
    private void tryFireConsoleProcessExited(Integer nConsoleProcessExitCode) {
        if (getConsoleProcessExitCode() != null) // It's OK to call it from multiple places, e.g. when payload exit were detected and when ConEmu process itself exits
            return;

        // Make sure the whole ANSI log contents is pumped out before we notify user
        // Dispose call pumps all out and makes sure we never ever fire anything on it after we notify user of ConsoleProcessExited; multiple calls to Dispose are OK
        if (ansilog != null)
            ansilog.dispose();

        // Store exit code
        this.nConsoleProcessExitCode = nConsoleProcessExitCode;

        // Notify user
        consoleProcessExited(this, new ConsoleProcessExitedEvent(nConsoleProcessExitCode));
    }

    private void consoleProcessExited(ConEmuSession source, ConsoleProcessExitedEvent event) {
        if (emulatorClosedEventListeners != null)
            for (ConsoleProcessExitedListener l : processExitedEventListeners) {
                l.processExited(source, event);
            }
    }

    private void addConsoleEmulatorClosedEventSink(ConsoleEmulatorClosedListener consoleEmulatorClosedEventSink) {
        this.emulatorClosedEventListeners.add(consoleEmulatorClosedEventSink);
    }

    private void addConsoleProcessExitedEventSink(ConsoleProcessExitedListener consoleProcessExitedEventSink) {
        processExitedEventListeners.add(consoleProcessExitedEventSink);
    }

    private void consoleEmulatorClosed(Object source, ConsoleEmulatorClosedEvent event) {
        if (emulatorClosedEventListeners != null)
            for (ConsoleEmulatorClosedListener l : emulatorClosedEventListeners) {
                l.emulatorClosed(source, event);
            }
    }

    public interface ConsoleEmulatorClosedListener extends EventListener {
        void emulatorClosed(Object source, ConsoleEmulatorClosedEvent event);
    }

    /**
     * <p>Fires on the main thread when the console emulator closes and the console emulator window is hidden from the control.</p>
     * <p>Note that the console process might have terminated long before this moment without closing the console emulator unless {@link WhenConsoleProcessExits#CloseConsoleEmulator} were selected in the startup options.</p>
     * <p>For short-lived processes, this event might fire before you can start sinking it. To get notified reliably, use {@link #waitForConsoleEmulatorCloseAsync()} or {@link ConEmuStartInfo#getConsoleEmulatorClosedEventSink()} .</p>
     */
    public class ConsoleEmulatorClosedEvent extends EventObject {
        public ConsoleEmulatorClosedEvent(Object source) {
            super(source);
        }
    }

    public interface ConsoleProcessExitedListener extends EventListener {
        void processExited(Object source, ConsoleProcessExitedEvent event);
    }

    /**
     * <p>Fires on the main thread when the console process running in the console emulator terminates.</p>
     * <p>If not {@link WhenConsoleProcessExits#CloseConsoleEmulator}, the console emulator stays, otherwise it closes also, and the console emulator window is hidden from the control.</p>
     * <p>For short-lived processes, this event might fire before you can start sinking it. To get notified reliably, use {@link #waitForConsoleEmulatorCloseAsync()} or {@link ConEmuStartInfo#getConsoleProcessExitedEventSink()}.</p>
     */
    public class ConsoleProcessExitedEvent extends EventObject {
        private final int exitCode;

        public ConsoleProcessExitedEvent(int exitcode) {
            super(exitcode);
            exitCode = exitcode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }

    /**
     * Covers parameters of the host control needed to run the session. {@link ConEmuStartInfo} tells what to run and how, while this class tells “where” and is not directly user-configurable, it's derived from the hosting control.
     */
    public class HostContext {
        public HostContext(@NotNull WinDef.HWND hWndParent, boolean isStatusbarVisibleInitial) {
            if (hWndParent == null)
                throw new NullArgumentException("hWndParent");
            HWndParent = hWndParent;
            IsStatusbarVisibleInitial = isStatusbarVisibleInitial;
        }

        @NotNull
        public WinDef.HWND HWndParent;

        public boolean IsStatusbarVisibleInitial;
    }

}
