package org.iceterm.jni;

import javax.swing.*;

public class ConEmuPipeServer_N {
    static {
        System.loadLibrary("iceterm");
    }

    public static int keyEventReceied(int key) {
        JOptionPane.showMessageDialog(null, "Received " + key);
        return key;
    }

    public int runPipeServer() {
        return this.N_RunPipeServer();
    }

    private final native int N_RunPipeServer();
}
