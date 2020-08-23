package org.iceterm.action;

import com.intellij.ide.actions.RevealFileAction;
import com.intellij.ide.actions.ShowFilePathAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.iceterm.IceTermView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenInNewTab extends DumbAwareAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = getEventProject(e);
        e.getPresentation().setEnabledAndVisible(project != null && getSelectedFile(e) != null);
    }

    @Nullable
    private static VirtualFile getSelectedFile(@NotNull AnActionEvent e) {
        return RevealFileAction.findLocalFile(e.getData(CommonDataKeys.VIRTUAL_FILE));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = getEventProject(e);
        VirtualFile selectedFile = getSelectedFile(e);
        if (project == null || selectedFile == null) {
            return;
        }
        IceTermView iceTermView = IceTermView.getInstance(project);
        if(iceTermView != null) {
            iceTermView.openTerminalIn(selectedFile, true);
        }
    }
}
