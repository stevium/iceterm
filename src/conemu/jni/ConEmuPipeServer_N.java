package conemu.jni;

import javax.swing.*;

public class ConEmuPipeServer_N {
    static {
        System.loadLibrary("ConEmuPipeServer_N");
    }

    public static int keyEventReceied(int key) {
        JOptionPane.showMessageDialog(null, key);
        return key;
    }

    public static native int runPipeServer();
}
