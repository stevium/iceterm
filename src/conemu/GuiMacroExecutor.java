package conemu;

import conemu.jni.GuiMacroExecutor_N;
import conemu.util.CommandLineBuilder;
import conemu.util.tasks.Task;
import conemu.util.tasks.TaskCompletionSource;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * <p>Implements calling GuiMacro to the remote ConEmu instance, and getting the result.</p>
 * <p>Got switching implementation for out-of-process (classic, via a console tool) and in-process (new feature which loads the helper comm DLL directly) access.</p>
 */
public class GuiMacroExecutor implements AutoCloseable {

    /*
     * Prevent unloads when async calls are being placed.
     */
    private final Object lock = new Object();
    private GuiMacroExecutor_N guiMacroExecutor_N = null;
    private Long _hConEmuCd;
    private Long _fnGuiMacro;

    /**
     * Inits the object, loads the extender DLL if known. If <code>NULL</code>, in-process operations will not be available.
     */
    public GuiMacroExecutor(@Nullable String asLibrary) {
        if (asLibrary == null || StringUtils.isEmpty(asLibrary)) {
            LoadConEmuDll(asLibrary);
        }
    }

    public Task<GuiMacroResult> executeInProcessAsync(int nConEmuPid, @NotNull String asMacro) {
        if (asMacro == null)
            throw new NullArgumentException("asMacro");
        if (_hConEmuCd == null) //
            throw new IllegalStateException("ConEmuCD war not loaded.");

        TaskCompletionSource<GuiMacroResult> tasker = new TaskCompletionSource<>();

        // Bring the call on another thread, because placing the call on the same thread as ConEmu might cause a deadlock when it's still in the process of initialization
        // (the GuiMacro stuff was designed for out-of-process comm and would blocking-wait for init to complete)
        Thread thread = new Thread(() -> {
            if (_fnGuiMacro == null)
                throw new IllegalStateException("The function pointer has not been bound.");

            String sResult = null;
            int iRc = guiMacroExecutor_N.executeInProcess(String.valueOf(nConEmuPid), asMacro);

            switch (iRc) {
                case 0: // This is expected
                case 133:
                    tasker.setResult(new GuiMacroResult(true, sResult));
                    break;
                case 134:
                    tasker.setResult(new GuiMacroResult(false));
                    break;
                default:
                    tasker.setError(new IllegalStateException("Internal ConEmuCD error: " + iRc));
            }

        });

        thread.start();
        return tasker.getTask();
    }

    public Task<GuiMacroResult> executeViaExtenderProcessAsync(@NotNull String macrotext, int nConEmuPid, @NotNull String sConEmuConsoleExtenderExecutablePath) {
        if (macrotext == null)
            throw new NullArgumentException("macrotext");
        if (sConEmuConsoleExtenderExecutablePath == null)
            throw new NullArgumentException("sConEmuConsoleExtenderExecutablePath");

        // conemuc.exe -silent -guimacro:1234 print("\e","git"," --version","\n")

        CommandLineBuilder cmdl = new CommandLineBuilder();
        cmdl.appendSwitch("-silent");
        cmdl.appendSwitchIfNotNull("-GuiMacro:", String.valueOf(nConEmuPid));
        cmdl.appendSwitch(macrotext /* appends the text unquoted for cmdline */);

        if (sConEmuConsoleExtenderExecutablePath == "")
            throw new IllegalStateException("The ConEmu Console Extender Executable is not available.");
        if (!(new File(sConEmuConsoleExtenderExecutablePath).exists()))
            throw new IllegalStateException("The ConEmu Console Extender Executable does not exist on disk at " + sConEmuConsoleExtenderExecutablePath + ".");


        Task<Task<GuiMacroExecutor>> taskStart = new TaskCompletionSource<Task<GuiMacroExecutor>>().getTask();

        Thread t = new Thread(() -> {
            ProcessBuilder builder = new ProcessBuilder();
            // TODO run hidden
            builder.command(sConEmuConsoleExtenderExecutablePath, cmdl.toString());
            try {
                builder.start();
                StringBuilder sbResult = new StringBuilder();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return null;
    }

    private void LoadConEmuDll(@NotNull String asLibrary) {
        if (asLibrary == null)
            throw new NullArgumentException("asLibrary");
        if (_hConEmuCd != null)
            return;

        guiMacroExecutor_N = new GuiMacroExecutor_N();
        _hConEmuCd = guiMacroExecutor_N.loadConEmuDll(asLibrary);
        if (_hConEmuCd == null) {
            throw new IllegalStateException("Can't load library " + asLibrary);
        }


        _fnGuiMacro = guiMacroExecutor_N.initGuiMacroFn();
        if (_fnGuiMacro == null) {
            throw new IllegalStateException("Function GuiMacro not found in library\n" + asLibrary + "\nUpdate ConEmu modules");
        }
    }

    private void UnloadConEmuDll() {
        if (_hConEmuCd != null) {
            // TODO
        }
    }

    @Override
    public void close() {
        //TODO
//        lock(lock)
        UnloadConEmuDll();
//        GC.SuppressFinalize(this);
    }
}
