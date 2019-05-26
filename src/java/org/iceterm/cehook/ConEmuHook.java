package org.iceterm.cehook;

import com.sun.jna.platform.win32.WinDef;

import javax.swing.*;

public class ConEmuHook {
    static {
        System.loadLibrary("iceterm");
    }

    public static void dataReceived(String message) {
        System.out.println("Received " + message);
        JOptionPane.showMessageDialog(null, "Received " + message);
    }

    public void run(int conEmuPid, long wHwnd) {
        try {
            Thread serverThread = new Thread(() -> {
                try {
                    runPipeServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();
            inject(conEmuPid, wHwnd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static native void runPipeServer();
    private static native void inject(int nConEmuPid, long wHwnd);
}
