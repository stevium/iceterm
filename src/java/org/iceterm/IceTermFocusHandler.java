package org.iceterm;

import com.intellij.ide.actions.ActivateToolWindowAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import org.iceterm.ceintegration.ConEmuControl;
import org.iceterm.util.ToolWindowUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

public class IceTermFocusHandler implements AnActionListener, ToolWindowManagerListener, AWTEventListener {

    private final ToolWindowUtils myToolWindowUtils;
    private final Project myProject;
    boolean wasVisible = false;

    private final ToolWindow myToolWindow;
    private final ConEmuControl myConEmuControl;

    public IceTermFocusHandler(ConEmuControl conEmuControl, ToolWindow toolWindow, Project project, ToolWindowUtils toolWindowUtils) {
        this.myConEmuControl = conEmuControl;
        this.myToolWindow = toolWindow;
        this.myToolWindowUtils = toolWindowUtils;
        this.myProject = project;
    }

    @Override
    public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, AnActionEvent event) {
        if (action instanceof ActivateToolWindowAction) {
            if (((ActivateToolWindowAction) action).getToolWindowId().equals(myToolWindow.getId())) {
                Thread t = new Thread(() -> {
                    try {
                        Thread.sleep(20);
                        myConEmuControl.setFocus();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                t.start();
            }
        }
    }

    @Override
    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        if (myToolWindow.isDisposed()) return;
        boolean visible = myToolWindow.isVisible();
        if (myToolWindowUtils.isInToolWindow(toolWindowManager.getFocusManager().getFocusOwner())) {
            myConEmuControl.setFocus();
        }

        if (!visible) {
            wasVisible = false;
            return;
        }

        if(!wasVisible) {
            myConEmuControl.setFocus();
            wasVisible = true;
        }
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        if (myConEmuControl == null) {
            return;
        }

        if (event.getID() == 1004) {
            FocusEvent focusEvent = (FocusEvent) event;
            if (myToolWindowUtils.isInToolWindow(focusEvent.getSource())) {
                ToolWindowManager windowManager = ToolWindowManager.getInstance(myProject);
                windowManager.invokeLater(() -> {
                    JComponent parent = (JComponent) myConEmuControl.getParent();
                    parent.getRootPane().getParent().setVisible(true);
                });
                Thread t = new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        System.out.println("Settings Focus from 1004");
                        myConEmuControl.setFocus();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                t.start();
            }
        }

        if (event.getID() == 501 && myConEmuControl.isForeground()) {
            MouseEvent mouseEvent = (MouseEvent) event;

            if (myToolWindowUtils.isInToolWindow(mouseEvent.getComponent())) {
                return;
            }

            myConEmuControl.removeFocus();
        }
    }
}
