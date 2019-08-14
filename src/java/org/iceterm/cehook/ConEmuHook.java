package org.iceterm.cehook;

import org.iceterm.IceTermOptionsProvider;
import org.iceterm.cehook.keyboard.NativeKeyEvent;

import java.awt.event.InputEvent;

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
                    convertModifiers(options.getPrefixKey().getModifiers()),
                    options.getPrefixKey().getKeyCode(),
                    options.getPrefixKey().getKeyCode(),
                    options.getPrefixKey().getKeyChar()));

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

    public int convertModifiers(int modifiers) {
        int newModifiers = 0;
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            newModifiers |= NativeKeyEvent.SHIFT_MASK;
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            newModifiers |= NativeKeyEvent.ALT_MASK;
        }
        if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
            newModifiers |= NativeKeyEvent.ALT_MASK;
        }
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            newModifiers |= NativeKeyEvent.CTRL_MASK;
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0) {
            newModifiers |= NativeKeyEvent.META_MASK;
        }

        return newModifiers;
    }

}
