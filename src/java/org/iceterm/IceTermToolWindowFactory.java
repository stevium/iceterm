package org.iceterm;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

public class IceTermToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String TOOL_WINDOW_ID = "IdeaConEmu";

    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        IceTermView terminalView = IceTermView.getInstance(project);
        terminalView.initToolWindow(toolWindow);
    }
}

