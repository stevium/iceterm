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

    /**
     * Inits the object, loads the extender DLL if known. If <code>NULL</code>, in-process operations will not be available.
     */
    public GuiMacroExecutor(@Nullable String asLibrary) {
        if (asLibrary == null || StringUtils.isEmpty(asLibrary)) {
            guiMacroExecutor_N = new GuiMacroExecutor_N();
            guiMacroExecutor_N.loadConEmuDll(asLibrary);
        }
    }

    public Task<GuiMacroResult> executeInProcessAsync(int nConEmuPid, @NotNull String asMacro) {
        if(asMacro == null)
            throw new NullArgumentException("asMacro");


        return null;
    }

    public Task<GuiMacroResult> executeViaExtenderProcessAsync(@NotNull String macrotext, int nConEmuPid, @NotNull String sConEmuConsoleExtenderExecutablePath) {
        if(macrotext == null)
            throw new NullArgumentException("macrotext");
        if(sConEmuConsoleExtenderExecutablePath == null)
            throw new NullArgumentException("sConEmuConsoleExtenderExecutablePath");

        // conemuc.exe -silent -guimacro:1234 print("\e","git"," --version","\n")

        CommandLineBuilder cmdl = new CommandLineBuilder();
        cmdl.appendSwitch("-silent");
        cmdl.appendSwitchIfNotNull("-GuiMacro:", String.valueOf(nConEmuPid));
        cmdl.appendSwitch(macrotext /* appends the text unquoted for cmdline */);

        if(sConEmuConsoleExtenderExecutablePath == "")
            throw new IllegalStateException("The ConEmu Console Extender Executable is not available.");
        if(!(new File(sConEmuConsoleExtenderExecutablePath).exists()))
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

    @Override
    public void close() {
//        lock(lock)
//        UnloadConEmuDll();
//        GC.SuppressFinalize(this);
    }

//    public class GuiMacroException extends Exception {
//        public GuiMacroException(String asMessage) {
//            super(asMessage);
//        }
//    }
}
