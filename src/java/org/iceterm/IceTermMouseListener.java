package org.iceterm;

import com.intellij.openapi.wm.ToolWindow;
import org.iceterm.cehook.mouse.NativeMouseEvent;
import org.iceterm.cehook.mouse.NativeMouseListener;
import org.iceterm.ceintegration.ConEmuControl;

public class IceTermMouseListener implements NativeMouseListener {
    private final ConEmuControl conEmuControl;
    private final ToolWindow myToolWindow;

    public IceTermMouseListener(ToolWindow toolWindow, ConEmuControl conEmuControl) {
        this.conEmuControl = conEmuControl;
        this.myToolWindow = toolWindow;
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent nativeEvent) {
        System.out.println("entered nativeMouseClicked");
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent nativeEvent) {
        if(conEmuControl.isForeground()) {
            this.myToolWindow.getComponent().requestFocus();
        }
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent nativeEvent) {
        System.out.println("entered  nativeMouseReleased");
    }
}
