package conemu.jni;

public class GuiMacroExecutor_N {
    static {
        System.loadLibrary("IceTermJNI");
//        System.loadLibrary("GuiMacroExecutor_N");
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

    private final native long N_LoadConEmuDll(String asLibrary);

    private final native long N_InitGuiMacroFn();

    private final native int N_ExecuteInProcess(String nConEmuPid, String asMacro, StringBuffer result);
}

