package org.iceterm;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

public class IceTermToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String TOOL_WINDOW_ID = "IceTerm";

    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        IceTermView terminalView = IceTermView.getInstance(project);
        if(terminalView != null) {
            terminalView.initToolWindow(toolWindow);
        }
    }
}

