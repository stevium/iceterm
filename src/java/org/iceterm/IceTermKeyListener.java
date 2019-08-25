package org.iceterm;

import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.sun.jna.platform.win32.WinDef;
import org.iceterm.cehook.keyboard.SwingKeyAdapter;
import org.iceterm.ceintegration.ConEmuControl;
import org.iceterm.util.User32Ext;

import javax.swing.*;

import static com.sun.jna.Native.getComponentPointer;

import java.awt.event.KeyEvent;

public class IceTermKeyListener extends SwingKeyAdapter {

    private ConEmuControl conEmuControl;

    public IceTermKeyListener(ConEmuControl conemu) {
        conEmuControl = conemu;
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
//        WinDef.HWND hwnd = new WinDef.HWND(getComponentPointer(((JComponent)conEmuControl.getParent()).getRootPane()));
//        User32Ext.INSTANCE.SetForegroundWindow(hwnd);
//        User32Ext.INSTANCE.SetFocus(hwnd);
//        User32Ext.INSTANCE.SetActiveWindow(hwnd);
//        User32Ext.INSTANCE.SwitchToThisWindow(hwnd, true);
        conEmuControl.removeFocus();
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) { }
}
