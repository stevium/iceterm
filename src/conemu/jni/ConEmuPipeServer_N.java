package conemu.jni;

import javax.swing.*;

public class ConEmuPipeServer_N {
    static {
        System.loadLibrary("IceTermJNI");
    }

    public static int keyEventReceied(int key) {
        JOptionPane.showMessageDialog(null, "Received " + key);
        return key;
    }

    public static native int runPipeServer();
}
