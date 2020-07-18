package org.iceterm.action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.iceterm.IceTermKeyListener;
import org.iceterm.IceTermMouseListener;
import org.iceterm.IceTermToolWindowFactory;
import org.iceterm.IceTermView;
import org.iceterm.cehook.ConEmuHook;
import org.iceterm.cehook.GlobalScreen;
import org.iceterm.cehook.dispatcher.SwingDispatchService;
import org.iceterm.ceintegration.ConEmuControl;
import org.iceterm.ceintegration.ConEmuSession;
import org.iceterm.ceintegration.ConEmuStartInfo;
import org.iceterm.ceintegration.States;
import org.iceterm.util.WinApi;

public class ConEmuStateChangedListener implements ConEmuControl.StateChangedListener {

    private Project myProject;
    private VirtualFile fileToOpen;
    private ConEmuControl conEmuControl;

    public ConEmuStateChangedListener(Project myProject) {
        this.myProject = myProject;
    }

    public void setFileToOpen(VirtualFile fileToOpen) {
        this.fileToOpen = fileToOpen;
    }

    @Override
    public void stateChanged() {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(IceTermToolWindowFactory.TOOL_WINDOW_ID);
        States state = getConEmuControl().getState();
        if (state == States.ConsoleEmulatorWithConsoleProcess) {
            setUpHook();
            if(fileToOpen != null) {
                changeDir(fileToOpen, false);
                fileToOpen = null;
            }
        }
        if (state == States.Recycled) {
            disposeSession(window);
        }
    }

    private void disposeSession(ToolWindow window) {
        ToolWindowManager windowManager = ToolWindowManager.getInstance(myProject);
        windowManager.invokeLater(() -> {
            windowManager.getToolWindow(window.getId()).hide(null);
            getConEmuControl().invalidateSession();
            // if settings change update start info, so changes would apply without IDE  restart
            getConEmuControl().setStartInfo(new ConEmuStartInfo(myProject));
        });
    }

    private void setUpHook() {
        Process process = getConEmuControl().getSession().getProcess();
        ConEmuHook hook = new ConEmuHook();
        System.out.println("Setting up native hook hook");
        hook.run(WinApi.Helpers.getProcessId(process));
        GlobalScreen.addNativeKeyListener(new IceTermKeyListener(getConEmuControl()));
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(IceTermToolWindowFactory.TOOL_WINDOW_ID);
        IceTermMouseListener mouseListener = new IceTermMouseListener(window, getConEmuControl());
        GlobalScreen.addNativeMouseListener(mouseListener);
        GlobalScreen.setEventDispatcher(new SwingDispatchService());
    }

    public void changeDir(VirtualFile fileToOpen, Boolean newTab) {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(IceTermToolWindowFactory.TOOL_WINDOW_ID);
        if (window != null && window.isAvailable()) {
            ConEmuSession session = getConEmuControl().getSession();
            String path = fileToOpen.isDirectory() ? fileToOpen.getPath() : fileToOpen.getParent().getPath();
            path = path.replace("\"", "\"\"");
            if(newTab)  {
               session.ExecuteGuiMacroTextSync("Recreate(0)");
            }
            session.ExecuteGuiMacroTextSync("Print(@\"cd \"\"" + path + "\"\"\",\"\n\")");
        }
    }

    private ConEmuControl getConEmuControl() {
        if(conEmuControl == null) {
            IceTermView iceTermView = IceTermView.getInstance(myProject);
            if(iceTermView != null) {
                conEmuControl = iceTermView.getConEmuControl();
            }
        }
        return conEmuControl;
    }

}
