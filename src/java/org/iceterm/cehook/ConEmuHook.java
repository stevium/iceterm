package org.iceterm.cehook;

import org.iceterm.IceTermOptionsProvider;
import org.iceterm.cehook.keyboard.NativeKeyEvent;
import org.iceterm.ceintegration.ConEmuStartInfo;

public class ConEmuHook {
    static {
        try {
            System.load(ConEmuStartInfo.getIceTermDllPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(int conEmuPid) {
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
            inject(conEmuPid, new ConEmuStartInfo().getCeHookDllPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static native void createPipeServer();
    private static native void runPipeServer();
    private static native void inject(int nConEmuPid, String libPath);
}
