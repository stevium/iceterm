package conemu.jni;

public class GuiMacroExecutor_N {
    static {
        System.loadLibrary("GuiMacroExecutor_N");
    }

    public long loadConEmuDll(String asLibrary) {
        try {
            return N_LoadConEmuDll(asLibrary);
        }  catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return -1;
    }

    public long initGuiMacroFn() {
        try {
            return N_InitGuiMacroFn();
        }  catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return -1;
    }

    public int executeInProcess(int nConEmuPid, String asMacro) {
       try {
           return N_ExecuteInProcess(nConEmuPid, asMacro);
       }  catch (UnsatisfiedLinkError ule) {
           ule.printStackTrace();
           return -1;
       }
    }

    private final native long N_LoadConEmuDll(String asLibrary);
    private final native long N_InitGuiMacroFn();
    private final native int N_ExecuteInProcess(int nConEmuPid, String asMacro);
}

