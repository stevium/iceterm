package org.iceterm;

import org.iceterm.cehook.mouse.NativeMouseEvent;
import org.iceterm.cehook.mouse.NativeMouseListener;
import org.iceterm.ceintegration.ConEmuControl;

public class IceTermMouseListener implements NativeMouseListener {
    private final ConEmuControl conEmuControl;

    public IceTermMouseListener(ConEmuControl conEmuControl) {
        this.conEmuControl = conEmuControl;
    }

    @Override
    public void nativeMouseClicked(NativeMouseEvent nativeEvent) {
        System.out.println("entered nativeMouseClicked");
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent nativeEvent) {
        conEmuControl.getParent().requestFocus();
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent nativeEvent) {
        System.out.println("entered  nativeMouseReleased");
    }
}
