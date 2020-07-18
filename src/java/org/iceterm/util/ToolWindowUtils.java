package org.iceterm.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.impl.ProjectFrameHelper;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;

public class ToolWindowUtils {
    private final ToolWindow myToolWindow;
    private final Project myProject;

    public ToolWindowUtils(ToolWindow toolWindow, Project project) {
        this.myToolWindow = toolWindow;
        this.myProject = project;
    }

    public boolean isInToolWindow(Object component) {
        Component source = component instanceof Component ? (Component) component : null;

        if (source == null) {
            return false;
        }

        Component myToolWindow = this.myToolWindow.getComponent().getParent();

        if(myToolWindow == null) {
            return false;
        }

        if (((JComponent) myToolWindow).getRootPane() == null)
            return false;

        JFrame mainFrame = getMainFrame();
        Container myToolWindowRoot = ((JComponent) myToolWindow).getRootPane().getParent();

        if (source instanceof JFrame && source != mainFrame) {
            return source == myToolWindowRoot;
        }

        if (myToolWindowRoot != mainFrame)
            myToolWindow = myToolWindowRoot;

        if (myToolWindow != null) {
            while (source != null && source != myToolWindow) {
                source = source.getParent();
            }
        }

        return source != null;
    }

    public JFrame getMainFrame() {
        Frame[] frames = Frame.getFrames();
        for (Frame frame : frames) {
            if (frame instanceof IdeFrame) {
                ProjectFrameHelper frameHelper = ProjectFrameHelper.getFrameHelper(frame);

                if (frameHelper == null || frameHelper.getProject() == null) {
                    continue;
                }

                if (StringUtils.equals(frameHelper.getProject().getName(), myProject.getName())) {
                    return (JFrame) frame;
                }
            }
        }
        return null;
    }


}
