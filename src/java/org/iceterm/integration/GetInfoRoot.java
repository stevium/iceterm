package org.iceterm.integration;

import org.iceterm.util.tasks.Task;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

public class GetInfoRoot {

   /**
     * Handles the <code>GetInfo</code> GuiMacro for the <code>Root</code> command.
     */
    private GetInfoRoot(States state, @NotNull String name, Integer pid, Integer exitCode) {
        if (name == null)
            throw new NullArgumentException("name");
        this.name = name;
        this.pid = pid;
        this.state = state;
        this.exitCode = exitCode;
    }

    /**
     * Exit code of the payload process, if it has exited.
     */
    public final Integer exitCode;
    /**
     * Name of the process, should always be available whether it's running or not (yet/already).
     */
    @NotNull
    private final String name;
    /**
     * The process ID, available only when the payload process is running.
     */
    public final Integer pid;
    /**
     * The current state of the root console process, as in the <code>GetInfo Root</code> GUI Macro <code>State</code> field: Empty, NotStarted, Running, Exited.
     */
    private final States state;

    public boolean isRunning() {
        return state == States.Running;
    }

    @NotNull
    public static Task<GetInfoRoot> queryAsync(@NotNull ConEmuSession session) {
        if (session == null)
            throw new NullArgumentException("session");

        return session.beginGuiMacro("GetInfo").withParam("Root").executeAsync().continueWith(task -> {
            GuiMacroResult result = task.getResult();
            if (!result.isSuccessful)
                throw new IllegalStateException("The GetInfo-Root call did not succeed.");
            if (result.response == null || StringUtils.isEmpty(result.response)) // Might yield an empty string randomly if not ready yet
                throw new IllegalStateException("The GetInfo-Root call has yielded an empty result.");

            // Interpret the string as XML
            Document document;
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = null;
                builder = factory.newDocumentBuilder();
                document = builder.parse(new InputSource(new StringReader(result.response)));
            } catch (SAXException | IOException | ParserConfigurationException e) {
                // Could not parse the XML response. Not expected. Wait more.
                throw new IllegalStateException("The GEtInfo-Root call result " + result.response + " were not a valid XML document.", e);
            }
            Element xmlRoot = document.getDocumentElement();
            if (xmlRoot == null)
                throw new IllegalStateException("The GetInfo-Root call result " + result.response + " didn't have a root XML element.");

            // Current possible records:
            // <Root State="NotStarted" Name="cmd.exe" />
            // <Root State="Running" Name="cmd.exe" PID="4672" ExitCode="259" UpTime="4406" />
            // <Root State="Exited" Name="cmd.exe" PID="4672" ExitCode="0" UpTime="14364"/>
            // Also, there's the State="Empty" documented, though it would be hard to catch

            String sState = xmlRoot.getAttribute("State");
            if (StringUtils.isBlank(sState))
                throw new IllegalStateException("The GetInfo-Root call result " + result.response + " didn't specify the current ConEmu state.");
            States state;
            try {
                state = States.valueOf(sState);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("The GetInfo-Root call result " + result.response + " specifies the State " + sState + " which cannot be matched agains the list of the known states " + Arrays.toString(States.values()).replaceAll("^.|.$", ""));
            }

            String sName = xmlRoot.getAttribute("Name");

            int nPidRaw;
            Integer nPid = null;
            if (state == States.Running) {
                try {
                    nPidRaw = Integer.parseInt(xmlRoot.getAttribute("PID"));
                    nPid = nPidRaw;
                } catch (NumberFormatException ex) {
                    nPid = null;
                }
            }

            int nExitCodeRaw;
            Integer nExitCode = null;
            if (state == States.Exited) {
                try {
                    nExitCodeRaw = Integer.parseInt(xmlRoot.getAttribute("ExitCode"));
                    nExitCode = nExitCodeRaw;
                } catch (NumberFormatException ex) {
                    nExitCode = null;
                }
            }

            return new GetInfoRoot(state, sName, nPid, nExitCode);
        });
    }

    /**
     * State: Empty, NotStarted, Running, Exited.
     */
    public enum States {
        /**
         * If there are not consoles in ConEmu.
         */
        Empty,
        /**
         * If console initialization is in progress (<code>ping localhost -t</code> for examle).</c>
         */
        NotStarted,
        /**
         * If root process was started and is running. Note, <code>259</code> in <code>ExitCode</code> is <code>STILL_ACTIVE</code> constant.
         */
        Running,
        /**
         * <p>• If root process was finished (terminated by `Ctrl+C` as example).</p>
         * <p>• Another example for `cmd.exe` normal exit.</p>
         */
        Exited
    }
}
