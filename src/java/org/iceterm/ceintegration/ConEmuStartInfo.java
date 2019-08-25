package org.iceterm.ceintegration;

import com.intellij.openapi.project.Project;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.iceterm.IceTermOptionsProvider;
import org.iceterm.IceTermProjectOptionsProvider;
import org.iceterm.util.FileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ConEmuStartInfo {
    public static String binFolder;
    public static String ICE_TERM = "iceterm";

    static {
        try {
            binFolder = extractBinaries();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private org.w3c.dom.Document baseConfiguration;

    @NotNull
    private TreeMap<String, String> environment = new TreeMap<>(Comparator.comparing(o -> o.toLowerCase(Locale.ROOT)));

    @Nullable
    private AnsiLog.AnsiStreamChunkReceivedListener evtAnsiStreamChunkReceivedEventSink;

    @Nullable
    private ConEmuSession.ConsoleEmulatorClosedListener evtConsoleEmulatorClosedEventSink;

    @Nullable
    private ConEmuSession.ConsoleProcessExitedListener evtConsoleProcessExitedEventSink;

    private boolean isEchoingConsoleCommandLine;

    private boolean isElevated;
    private Project myProject;

    public enum LogLevels {
        /**
         * Logging is disabled
         */
        Disabled,
        /**
         * Initial logging level, recommended. Implies switch "-Log"
         */
        Basic,
        /**
         * More data would be logging. Implies switch "-Log2"
         */
        Detailed,
        /**
         * Almost Full logging level. Implies switch "-Log3"
         */
        Andvanced,
        /**
         * Maximum logging level. Implies switch "-Log4"
         */
        Full
    }

    private LogLevels logLevel = LogLevels.Disabled;

    private boolean isReadingAnsiStream;

    private boolean isUsedUp;

    @NotNull
    private String sConEmuConsoleExtenderExecutablePath = "";

    @NotNull
    private String sConEmuConsoleServerExecutablePath = "";

    @NotNull
    private String sConEmuExecutablePath = "";

    @NotNull
    private String sConsoleProcessCommandLine = ConEmuConstants.DefaultConsoleCommandLine;

    @NotNull
    private String sGreetingText = "";

    @Nullable
    private String sStartupDirectory;

    private WhenConsoleProcessExits whenConsoleProcessExits = WhenConsoleProcessExits.KeepConsoleEmulatorAndShowMessage;

    public ConEmuStartInfo() {
        setConEmuExecutablePath(InitConEmuLocation());
    }

    public ConEmuStartInfo(Project myProject) {
        this();
        this.myProject = myProject;
        updateFromSettings();
    }

    public ConEmuStartInfo(@NotNull String sConsoleProcessCommandLine, Project myProject) {
        this(myProject);
        if (sConsoleProcessCommandLine == null)
            throw new NullArgumentException("sConsoleProcessCommandLine");

        setConsoleProcessCommandLine(sConsoleProcessCommandLine);
    }

    private void updateFromSettings() {
        IceTermOptionsProvider myOptionsProvider = IceTermOptionsProvider.getInstance();
        IceTermProjectOptionsProvider myProjectOptionsProvider = IceTermProjectOptionsProvider.getInstance(myProject);
        this.setsStartupDirectory(myProjectOptionsProvider.getStartingDirectory());
        this.setConsoleProcessCommandLine(myOptionsProvider.getShellTask());
        this.setConEmuExecutablePath(myOptionsProvider.getConEmuPath());
        this.setLogLevel(ConEmuStartInfo.LogLevels.Basic);
    }

    @Nullable
    public AnsiLog.AnsiStreamChunkReceivedListener getAnsiStreamChunkReceivedEventSink() {
        return evtAnsiStreamChunkReceivedEventSink;
    }

    public void setAnsiStreamChunkReceivedEventSink(@Nullable AnsiLog.AnsiStreamChunkReceivedListener evtAnsiStreamChunkReceivedEventSink) {
        AssertNotUsedUp();
        this.evtAnsiStreamChunkReceivedEventSink = evtAnsiStreamChunkReceivedEventSink;
    }

    @NotNull
    public Document getBaseConfiguration() {
        if (baseConfiguration != null)
            return baseConfiguration;

        try {
            InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(ICE_TERM + "/ConEmu.xml");
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            return baseConfiguration = documentBuilder.parse(new InputSource(resourceAsStream));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setBaseConfiguration(@NotNull Document baseConfigurationDocument) {
        if (baseConfigurationDocument == null)
            throw new NullArgumentException("baseConfigurationDocument");
        AssertNotUsedUp();
        baseConfiguration = baseConfigurationDocument;
    }

    @NotNull
    public String getConEmuConsoleExtenderExecetablePath() {
        return sConEmuConsoleExtenderExecutablePath;
    }

    public void setConEmuConsoleExtenderExecutablePath(@NotNull String consoleExtenderExPath) {
        if (consoleExtenderExPath == null)
            throw new NullArgumentException("consoleExtenderExPath");
        if ((consoleExtenderExPath.equals("")) && (sConEmuConsoleExtenderExecutablePath.equals("")))
            return;
        if (consoleExtenderExPath.equals(""))
            throw new IllegalArgumentException("consoleExtenderExPath" +
                    "Cannot reset path to an empty string.");

        sConEmuConsoleExtenderExecutablePath = consoleExtenderExPath;
    }

    @NotNull
    public String getConEmuConsoleServerExecutablePath() {
        return sConEmuConsoleServerExecutablePath;
    }

    public void setConEmuConsoleServerExecutablePath(@NotNull String consoleServerExPath) {
        if (consoleServerExPath == null)
            throw new NullArgumentException("consoleServerExPath");
        if ((consoleServerExPath.equals("")) && (sConEmuConsoleExtenderExecutablePath.equals("")))
            return;
        if (consoleServerExPath.equals(""))
            throw new IllegalArgumentException("consoleServerExPath" +
                    "Cannot reset path to an empty string.");

        sConEmuConsoleServerExecutablePath = consoleServerExPath;
    }

    @NotNull
    public String getConEmuExecutablePath() {
        return sConEmuExecutablePath;
    }

    public void setConEmuExecutablePath(@NotNull String conEmuExecutablePath) {
        if (conEmuExecutablePath == null)
            throw new NullArgumentException("conEmuExecutablePath");
        if ((conEmuExecutablePath.equals("")) && (sConEmuConsoleExtenderExecutablePath.equals("")))
            return;
        if (conEmuExecutablePath.equals(""))
            throw new IllegalArgumentException("conEmuExecutablePath" +
                    "Cannot reset path to an empty string.");
        sConEmuExecutablePath = conEmuExecutablePath;

        if (sConEmuConsoleExtenderExecutablePath.equals(""))
            sConEmuConsoleExtenderExecutablePath = TryDeriveConEmuConsoleExtenderExecutablePath(sConEmuExecutablePath);
        if (sConEmuConsoleServerExecutablePath.equals(""))
            sConEmuConsoleServerExecutablePath = TryDeriveConEmuConsoleServerExecutablePath(sConEmuExecutablePath);
    }

    @Nullable
    public ConEmuSession.ConsoleEmulatorClosedListener getConsoleEmulatorClosedEventSink() {
        return evtConsoleEmulatorClosedEventSink;
    }

    public void setConsoleEmulatorClosedEventSink(@NotNull ConEmuSession.ConsoleEmulatorClosedListener evtListener) {
        this.evtConsoleEmulatorClosedEventSink = evtListener;
    }

    @NotNull
    public String getConsoleProcessCommandLine() {
        return sConsoleProcessCommandLine;
    }

    public void setConsoleProcessCommandLine(String consoleProcCmd) {
        if (consoleProcCmd == null)
            throw new NullArgumentException("consoleProcCmd");
        AssertNotUsedUp();
        sConsoleProcessCommandLine = consoleProcCmd;
    }

    @Nullable
    public ConEmuSession.ConsoleProcessExitedListener getConsoleProcessExitedEventSink() {
        return evtConsoleProcessExitedEventSink;
    }

    public void setConsoleProcessExitedEventSink(@Nullable ConEmuSession.ConsoleProcessExitedListener evtListener) {
        this.evtConsoleProcessExitedEventSink = evtListener;
    }

    @NotNull
    public String getGreetingText() {
        return sGreetingText;
    }

    public void setGreetingText(@NotNull String greetingText) {
        if (greetingText == null)
            throw new NullArgumentException("greetingText");
        AssertNotUsedUp();
        sGreetingText = greetingText;
    }

    public boolean isEchoingConsoleCommandLine() {
        return isEchoingConsoleCommandLine;
    }

    public void setEchoingConsoleCommandLine(boolean echoingConsoleCommandLine) {
        AssertNotUsedUp();
        isEchoingConsoleCommandLine = echoingConsoleCommandLine;
    }

    public boolean isElevated() {
        return isElevated;
    }

    public void setElevated(boolean elevated) {
        AssertNotUsedUp();
        isElevated = elevated;
    }

    public LogLevels getLogLevel() {
        return LogLevels.Detailed;
    }

    public void setLogLevel(LogLevels logLevel) {
        AssertNotUsedUp();
        this.logLevel = logLevel;
    }

    public boolean isReadingAnsiStream() {
        return isReadingAnsiStream || evtAnsiStreamChunkReceivedEventSink != null;
    }

    public void setReadingAnsiStream(boolean readingAnsiStream) {
        AssertNotUsedUp();
        if ((!readingAnsiStream) && (evtAnsiStreamChunkReceivedEventSink != null))
            throw new IllegalArgumentException("readingAnsiStream - Cannot turn IsReadingAnsiStream off when AnsiStreamChunkReceivedEventsink has a non-NULL value because it implies on a True value for this property");
        isReadingAnsiStream = readingAnsiStream;
    }

    @Nullable
    public String getStartupDirectory() {
        return sStartupDirectory;
    }

    public void setsStartupDirectory(@Nullable String sStartupDirectory) {
        AssertNotUsedUp();
        this.sStartupDirectory = sStartupDirectory;
    }

    public WhenConsoleProcessExits getWhenConsoleProcessExits() {
        return whenConsoleProcessExits;
    }

    public void setWhenConsoleProcessExits(WhenConsoleProcessExits whenConsoleProcessExits) {
        AssertNotUsedUp();
        this.whenConsoleProcessExits = whenConsoleProcessExits;
    }

    @NotNull
    public Iterable<String> EnumEnv() {
        return environment.keySet();
    }

    @Nullable
    public String GetEnv(@NotNull String name) {
        if (name == null || name.isEmpty())
            throw new NullArgumentException("name");
        return environment.get(name);
    }

    public void SetEnv(@NotNull String name, @Nullable String value) {
        if (name == null || name.isEmpty())
            throw new NullArgumentException("name");
        AssertNotUsedUp();
        if (value == null)
            environment.remove(name);
        else {
            String envVariable = environment.get(name);
            envVariable = value;
        }
    }

    private void AssertNotUsedUp() {
        if (isUsedUp)
            throw new IllegalStateException("This change is not possible because the start info object has already been used up.");
    }

    @NotNull
    private String InitConEmuLocation() {
        String envPath = System.getenv("PATH");
        LinkedList<String> searhPaths = new LinkedList<String>();

        if (!StringUtils.isEmpty(binFolder))
            searhPaths.add(new File(binFolder, "ConEmu").getPath());

        searhPaths.addAll(Arrays.asList(envPath.split(";")));

        for (String dir : searhPaths) {
            File candidate = new File(dir, ConEmuConstants.ConEmuExeName);
            if (candidate.exists()) {
                return candidate.getAbsolutePath();
            }

            candidate = new File(new File(dir, ConEmuConstants.ConEmuSubfolderName), ConEmuConstants.ConEmuExeName);
            if (candidate.exists())
                return candidate.getPath();

        }

//        Logger.getInstance(this.getClass()).info("DIR IS " + dir);
        return "";
    }

    protected void MarkAsUsedUp() {
        isUsedUp = true;
    }

    @NotNull
    private String TryDeriveConEmuConsoleExtenderExecutablePath(
            @NotNull String sConEmuPath) {
        if (sConEmuPath == null)
            throw new NullArgumentException("sConEmuPath");
        if (sConEmuPath == "")
            return "";
        String dir = new File(sConEmuPath).getParent();
        if (dir == null || dir.isEmpty())
            return "";

        File candidate = new File(dir, ConEmuConstants.ConEmuConsoleExtenderExeName);
        if (candidate.exists())
            return candidate.getPath();

        candidate = new File(new File(dir, ConEmuConstants.ConEmuSubfolderName), ConEmuConstants.ConEmuConsoleExtenderExeName);
        if (candidate.exists())
            return candidate.getPath();

        return "";
    }

    @NotNull
    private String TryDeriveConEmuConsoleServerExecutablePath(@NotNull String sConEmuPath) {
        if (sConEmuPath == null)
            throw new NullArgumentException("sConEmuPath");
        if (sConEmuPath == "")
            return "";
        String dir = new File(sConEmuPath).getParent();
        if (dir == null || dir.isEmpty())
            return "";

        String sFileName = ConEmuConstants.ConEmuConsoleServerFileNameNoExt;
        sFileName += ".dll";

        File candidate = new File(dir, sFileName);
        if (candidate.exists())
            return candidate.getPath();

        candidate = new File(new File(dir, ConEmuConstants.ConEmuSubfolderName), sFileName);
        if (candidate.exists())
            return candidate.getPath();

        return "";
    }

    public static String getIceTermDllPath() {
        String ext = (System.getProperty("sun.arch.data.model").equals("64")) ? "64.dll" : ".dll";
        File icetermDll = new File(binFolder, "iceterm" + ext);
        if(icetermDll.exists())
            return icetermDll.getAbsolutePath();
        return "";
    }

    public static String getCeHookDllPath() {
        String ext = (System.getProperty("sun.arch.data.model").equals("64")) ? "64.dll" : ".dll";
        File cehookDll = new File(binFolder, "cehook" + ext);
        if(cehookDll.exists())
            return cehookDll.getAbsolutePath();
        return "";
    }

   public static String extractBinaries() throws IOException {
        File unzipDest = new File(FileHelper.getJarPath(ConEmuStartInfo.class), ICE_TERM + "-bin/");
        if(unzipDest.exists()) {
            return unzipDest.getPath();
        }

       InputStream in = ConEmuStartInfo.class.getClassLoader().getResourceAsStream(ICE_TERM + "/bin.zip");
       FileHelper.unzip(in, unzipDest.getPath());
        return unzipDest.getAbsolutePath();
    }
}

