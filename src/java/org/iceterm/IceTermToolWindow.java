package org.iceterm;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.swing.*;

public class IceTermToolWindow implements Disposable {
    public JPanel myToolWindowContent;

    public IceTermToolWindow(Project project) {
        Disposer.register(this, () -> {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            Disposer.dispose(toolWindowManager.getToolWindow(IceTermToolWindowFactory.TOOL_WINDOW_ID).getDisposable());
            myToolWindowContent = null;
        });
    }

    public JPanel getContent() {
        return myToolWindowContent;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    @Override
    public void dispose() {

    }
}

