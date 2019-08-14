package org.iceterm;

import org.iceterm.cehook.keyboard.SwingKeyAdapter;
import org.iceterm.ceintegration.ConEmuControl;

import java.awt.event.KeyEvent;

public class IceTermKeyListener extends SwingKeyAdapter {

    private ConEmuControl conEmuControl;

    public IceTermKeyListener(ConEmuControl conemu) {
        conEmuControl = conemu;
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        conEmuControl.removeFocus();
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) { }
}
