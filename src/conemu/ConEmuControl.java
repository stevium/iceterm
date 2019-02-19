package conemu;

import org.jetbrains.annotations.Nullable;

/**
 * <p>This is a console emulator control that embeds a fully functional console view in a Windows Forms window. It is capable of running any console application with full interactivity and advanced console functions. Applications will detect it as an actual console and will not fall back to the output redirection mode with reduced interactivity or formatting.</p>
 * <p>The control can be used to run a console process in the console emulator. The console process is the single command executed in the control, which could be a simple executable (the console emulator is not usable after it exits), or an interactive shell like <code>cmd</code> or <code>powershell</code> or <code>bash</code>, which in turn can execute multiple commands, either buy user input or programmatically with {@link ConEmuSession#writeInputText(String)}. The console emulator is what implements the console and renders the console view in the control. A new console emulator (represented by a {@link RunningSession} is {@link Start started} for each console process. After the root console process terminates, the console emulator might remain open {@link ConEmuStartInfo#getConsoleProcessCommandLine()} and still present the console window, or get closed. After the console emulator exits, the control is blank until {@link Start} spawns a new console emulator process in it. You cannot run more than one console emulator (console process) simultaneously.</p>
 */
public class ConEmuControl {
    /**
     * Enabled by default, and with all default values (runs the cmd shell).
     */
    private ConEmuStartInfo _autostartinfo = new ConEmuStartInfo();

    private boolean _isStatusbarVisible = true;

    /**
     * After the first console process exits (not session), stores it's exit code. Changes on the main thread only.
     */
    private Integer _nLastExitCode;

    /**
     * The running session, if currently running.
     */
    @Nullable
    private ConEmuSession _running;

}
