package org.iceterm.ceintegration;

import org.apache.commons.lang.NullArgumentException;
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
import java.security.CodeSource;
import java.util.Comparator;
import java.util.Locale;
import java.util.TreeMap;

public class ConEmuStartInfo {

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

    public ConEmuStartInfo(@NotNull String sConsoleProcessCommandLine) {
        this();
        if (sConsoleProcessCommandLine == null)
            throw new NullArgumentException("sConsoleProcessCommandLine");

        setConsoleProcessCommandLine(sConsoleProcessCommandLine);
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
            InputStream resourceAsStream = this.getClass().getResourceAsStream("/org/iceterm/ceintegration/ConEmu.xml");
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
    public Iterable<String> EnumEnv()
    {
        return environment.keySet();
    }

    @Nullable
    public String GetEnv(@NotNull String name)
    {
        if(name == null || name.isEmpty())
            throw new NullArgumentException("name");
        return environment.get(name);
    }

    public void SetEnv(@NotNull String name, @Nullable String value)
    {
        if(name == null || name.isEmpty())
            throw new NullArgumentException("name");
        AssertNotUsedUp();
        if(value == null)
            environment.remove(name);
        else {
            String envVariable = environment.get(name);
            envVariable = value;
        }
    }

    private void AssertNotUsedUp()
    {
        if(isUsedUp)
            throw new IllegalStateException("This change is not possible because the start info object has already been used up.");
    }

    @NotNull
    private String InitConEmuLocation()
    {
        String dir = null;

        try {
            dir = getContainingFolder(ConEmuStartInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(dir == null || dir.isEmpty())
            return "";

        File candidate = new File(dir, ConEmuConstants.ConEmuExeName);
        if(candidate.exists())
            return candidate.getPath();

        candidate = new File(new File(dir, ConEmuConstants.ConEmuSubfolderName), ConEmuConstants.ConEmuExeName);
        if(candidate.exists())
            return candidate.getPath();

        return "";
    }

    protected void MarkAsUsedUp()
    {
        isUsedUp = true;
    }

    @NotNull
    private String TryDeriveConEmuConsoleExtenderExecutablePath(
           @NotNull String sConEmuPath) {
        if(sConEmuPath == null)
            throw new NullArgumentException("sConEmuPath");
        if(sConEmuPath == "")
            return "";
        String dir = new File(sConEmuPath).getParent();
        if (dir == null || dir.isEmpty())
            return "";

        File candidate = new File(dir, ConEmuConstants.ConEmuConsoleExtenderExeName);
        if(candidate.exists())
            return candidate.getPath();

        candidate = new File(new File(dir, ConEmuConstants.ConEmuSubfolderName), ConEmuConstants.ConEmuConsoleExtenderExeName);
        if (candidate.exists())
            return candidate.getPath();

        return "";
    }

    @NotNull
    private String TryDeriveConEmuConsoleServerExecutablePath(@NotNull String sConEmuPath) {
        if(sConEmuPath == null)
            throw new NullArgumentException("sConEmuPath");
        if(sConEmuPath == "")
            return "";
        String dir = new File(sConEmuPath).getParent();
        if (dir == null || dir.isEmpty())
            return "";

        String sFileName = ConEmuConstants.ConEmuConsoleServerFileNameNoExt;
        sFileName += ".dll";

        File candidate = new File(dir, sFileName);
        if(candidate.exists())
            return candidate.getPath();

        candidate = new File(new File(dir, ConEmuConstants.ConEmuSubfolderName), sFileName);
        if (candidate.exists())
            return candidate.getPath();

        return "";
    }

    public static String getIcetermLibPath() throws Exception {
        return new File(getContainingFolder(ConEmuStartInfo.class),"native\\iceterm.dll").getAbsolutePath();
    }

    public static String getCeHookLibPath() throws Exception {
        return new File(getContainingFolder(ConEmuStartInfo.class),"native\\cehook.dll").getAbsolutePath();
    }

    private static String getContainingFolder(Class aclass) throws Exception {
        CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();

        File jarFile;

        if (codeSource.getLocation() != null) {
            jarFile = new File(codeSource.getLocation().toURI());
        } else {
            String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
//            String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
//            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
            jarFile = new File(path);
        }
        return jarFile.getParentFile().getAbsolutePath();
    }
}
