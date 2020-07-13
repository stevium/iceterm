package org.iceterm;

import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.FloatingDecorator;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.sun.jna.Pointer;
import org.iceterm.action.ConEmuStateChangedListener;
import org.iceterm.ceintegration.ConEmuControl;
import org.iceterm.ceintegration.ConEmuStartInfo;
import org.iceterm.util.ToolWindowUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.lang.reflect.Field;

import static com.sun.jna.Native.getComponentPointer;

public class IceTermView {

    private final IceTermToolWindow iceTermToolWindowPanel;
    private final Project myProject;
    private ToolWindow myToolWindow;
    private ConEmuStateChangedListener conEmuStateChangedListener;
    private ToolWindowUtils myToolWindowUtils;
    private IceTermFocusHandler focusHandler;

    public ConEmuControl getConEmuControl() {
        return conEmuControl;
    }

    private ConEmuControl conEmuControl;
    private Panel hiddenHandle;
    private JFrame frame;

    public IceTermView(@NotNull Project project) {
        myProject = project;
        iceTermToolWindowPanel = new IceTermToolWindow(project);
    }

    void initToolWindow(@NotNull ToolWindow toolWindow) {
        if (myToolWindow != null) {
            return;
        }

        myToolWindow = toolWindow;
        myToolWindowUtils = new ToolWindowUtils(myToolWindow, myProject);

        conEmuStateChangedListener = new ConEmuStateChangedListener(myProject);
        if (myToolWindow.getContentManager().getContentCount() == 0) {
            startNewSession();
        }
    }

    public static IceTermView getInstance(@NotNull Project project) {
        return project.getComponent(IceTermView.class);
    }

    public void openTerminalIn(VirtualFile fileToOpen, Boolean newTab) {
        if (fileToOpen == null) {
            return;
        }
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(IceTermToolWindowFactory.TOOL_WINDOW_ID);
        if (conEmuControl != null && conEmuControl.getSession() != null) {
            conEmuStateChangedListener.changeDir(fileToOpen, newTab);
            if (window != null) {
                window.show(null);
            }
            conEmuControl.setFocus();
            return;
        }
        initToolWindow(window);
        conEmuStateChangedListener.setFileToOpen(fileToOpen);
        window.show(null);
    }

    public void startNewSession() {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(IceTermToolWindowFactory.TOOL_WINDOW_ID);
        if (window != null && window.isAvailable()) {
            createTerminalContent(myToolWindow);
            focusHandler = new IceTermFocusHandler(conEmuControl, myToolWindow, myProject, myToolWindowUtils);
            addListeners();
            window.activate(null);
        }
    }

    private void createTerminalContent(ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(iceTermToolWindowPanel.getContent(), "", false);
        final ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);

        createConEmuControl();
    }

    private void createConEmuControl() {
        ConEmuStartInfo startInfo = new ConEmuStartInfo(myProject);
        conEmuControl = new ConEmuControl(startInfo, myToolWindow, myToolWindowUtils);
        conEmuControl.setMinimumSize(new Dimension(400, 400));
        iceTermToolWindowPanel.getContent().add(conEmuControl);

        Color background = iceTermToolWindowPanel.getContent().getParent().getBackground();
        iceTermToolWindowPanel.getContent().setBackground(background);
        conEmuControl.setBackground(background);
        conEmuControl.addStateChangedListener(conEmuStateChangedListener);
    }

    private void addListeners() {
        conEmuControl.addControlRemovedListener(this::saveTempHwnd);

        myProject.getMessageBus().connect().subscribe(AnActionListener.TOPIC, focusHandler);

        myProject.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, focusHandler);

        AWTEventListener internalEventLostListener = Toolkit.getDefaultToolkit().getAWTEventListeners()[2];
        Toolkit.getDefaultToolkit().removeAWTEventListener(internalEventLostListener);
        Toolkit.getDefaultToolkit().addAWTEventListener(this.focusHandler, 28L);
        Toolkit.getDefaultToolkit().addAWTEventListener(internalEventLostListener, AWTEvent.FOCUS_EVENT_MASK);
    }

    private void saveTempHwnd() {
        if (conEmuControl.getSession() == null) {
            return;
        }

        Frame[] frames = Frame.getFrames();

        if (hiddenHandle == null) {
            for (Frame frame : frames) {
                if (frame instanceof IdeFrameImpl) {
                    hiddenHandle = new Panel();
                    hiddenHandle.setVisible(false);
                    hiddenHandle.setLocation(400, 400);
                    hiddenHandle.setPreferredSize(new Dimension(1,1));
                    ((IdeFrameImpl) frame).getRootPane().add(hiddenHandle, BorderLayout.CENTER);
                }
            }
        }

        if (hiddenHandle != null) {
            Pointer pointer = getComponentPointer(hiddenHandle);
            conEmuControl.setParentHWND(pointer);
        }
    }

}
