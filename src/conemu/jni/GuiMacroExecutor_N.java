package conemu.jni;

import javax.swing.*;

public class GuiMacroExecutor_N {
    static {
        System.loadLibrary("IceTermJNI");
    }

    public Long loadConEmuDll(String asLibrary) {
        try {
            return N_LoadConEmuDll(asLibrary);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long initGuiMacroFn() {
        try {
            return N_InitGuiMacroFn();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer executeInProcess(String nConEmuPid, String asMacro, StringBuffer result) {
        try {
            return N_ExecuteInProcess(nConEmuPid, asMacro, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void runPipeServer() {
        try {
            Thread t = new Thread(() -> {
                try {
                    N_RunPipeServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            t.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runPipeClient(int pid) {
        try {
            N_RunPipeClient(pid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void keyEventReceived(String key) {
        JOptionPane.showMessageDialog(null, "Received " + key);
    }

    private final native long N_LoadConEmuDll(String asLibrary);

    private final native long N_InitGuiMacroFn();

    private final native int N_ExecuteInProcess(String nConEmuPid, String asMacro, StringBuffer result);

    private static native void N_RunPipeServer();

    private static native void N_RunPipeClient(int nConEmuPid);
}

