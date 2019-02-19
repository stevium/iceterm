package ideaconemu;

import java.awt.*;

public class JNIHelper {
    static {
        System.loadLibrary("JNIHelper");
    }

    public void setParent(Long childHandle, Long parentHandle) {
        try {
            N_SetParent(childHandle, parentHandle);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private native void N_SetParent(long hwnd, long handle);
}
