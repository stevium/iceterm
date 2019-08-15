package org.iceterm.cehook;

import org.iceterm.IceTermOptionsProvider;
import org.iceterm.cehook.keyboard.NativeKeyEvent;

public class ConEmuHook {
    static {
        System.loadLibrary("iceterm");
    }

    public void run(int conEmuPid, long wHwnd, int ideaPid) {
        try {
            createPipeServer();
            IceTermOptionsProvider options = IceTermOptionsProvider.getInstance();

            GlobalScreen.postNativeEvent(new NativeKeyEvent(
                    NativeKeyEvent.NATIVE_KEY_PRESSED,
                    AbstractSwingInputAdapter.getNativeModifiers(options.getEscapeKey().getModifiers()),
                    options.getEscapeKey().getKeyCode(),
                    options.getEscapeKey().getKeyCode(),
                    options.getEscapeKey().getKeyChar()));

            Thread serverThread = new Thread(() -> {
                try {
                    runPipeServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();
            inject(conEmuPid, wHwnd, ideaPid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static native void createPipeServer();
    private static native void runPipeServer();
    private static native void inject(int nConEmuPid, long wHwnd, int ideaPid);
}
