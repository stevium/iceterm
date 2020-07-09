package org.iceterm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.impl.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.sun.jna.Pointer;
import org.apache.commons.lang.StringUtils;
import org.iceterm.action.ConEmuStateChangedListener;
import org.iceterm.ceintegration.ConEmuControl;
import org.iceterm.ceintegration.ConEmuStartInfo;
import org.jetbrains.annotations.NotNull;

import static com.sun.jna.Native.getComponentPointer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

public class IceTermView {

    private final IceTermToolWindow iceTermToolWindowPanel;
    private final Project myProject;
    private ToolWindow myToolWindow;
    private ConEmuStateChangedListener conEmuStateChangedListener;

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

        conEmuStateChangedListener = new ConEmuStateChangedListener(myProject);
        if (myToolWindow.getContentManager().getContentCount() == 0) {
            createNewSession();
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
            conEmuControl.requestFocus();
            return;
        }
        initToolWindow(window);
        conEmuStateChangedListener.setFileToOpen(fileToOpen);
        window.show(null);
    }

    public void createNewSession() {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(IceTermToolWindowFactory.TOOL_WINDOW_ID);
        if (window != null && window.isAvailable()) {
            createTerminalContent(myToolWindow);
            window.activate(null);
        }
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

    private void createTerminalContent(ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(iceTermToolWindowPanel.getContent(), "", false);
        final ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);

        createConEmuControl();
        addListeners();
    }

    private void createConEmuControl() {
        ConEmuStartInfo startinfo = new ConEmuStartInfo(myProject);

        conEmuControl = new ConEmuControl(startinfo);
        conEmuControl.setMinimumSize(new Dimension(400, 400));
        iceTermToolWindowPanel.getContent().add(conEmuControl);
        Color background = iceTermToolWindowPanel.getContent().getParent().getBackground();
        iceTermToolWindowPanel.getContent().setBackground(background);
        conEmuControl.setBackground(background);
        conEmuControl.addStateChangedListener(conEmuStateChangedListener);
    }

    private void addListeners() {
        conEmuControl.addControlRemovedListener(this::saveTempHwnd);

        AWTEventListener listener = event -> {
            if (conEmuControl == null) {
                return;
            }

            if (event.getID() == 1004) {
                FocusEvent focusEvent = (FocusEvent) event;
                if (isInToolWindow(focusEvent.getSource())) {
                    ToolWindowManager windowManager = ToolWindowManager.getInstance(myProject);
                    windowManager.invokeLater(() -> {
                            JComponent parent = (JComponent) conEmuControl.getParent();
                            parent.getRootPane().getParent().setVisible(true);
                    });
                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(100);
                            System.out.println("Settings Focus from 1004");
                            conEmuControl.setFocus();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                    t.start();
                }
            }

            if (event.getID() == 501 && conEmuControl.isForeground()) {
                MouseEvent mouseEvent = (MouseEvent) event;
                String activeToolWindowId = ToolWindowManager.getInstance(myProject).getActiveToolWindowId();

                if (activeToolWindowId == null) {
                    return;
                }

                if (isInToolWindow(mouseEvent.getComponent())) {
                    return;
                }

                conEmuControl.removeFocus();
            }
        };
        AWTEventListener internalEventLostListener = Toolkit.getDefaultToolkit().getAWTEventListeners()[2];
        Toolkit.getDefaultToolkit().removeAWTEventListener(internalEventLostListener);
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, 28L);
        Toolkit.getDefaultToolkit().addAWTEventListener(internalEventLostListener, AWTEvent.FOCUS_EVENT_MASK);
    }

    private boolean isInToolWindow(Object component) {
        Component source = component instanceof Component ? (Component) component : null;

        if (source == null) {
            return false;
        }

        Component myToolWindow = this.myToolWindow.getComponent();

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

    private void saveTempHwnd() {
        if (conEmuControl.getSession() != null) {
            Component root = SwingUtilities.getRoot(myToolWindow.getComponent());
            if (root != null && frame == null) {
                if (root instanceof IdeFrame) {
                    frame = (JFrame) root;
                } else if (root instanceof FloatingDecorator) {
                    frame = (JFrame) root.getParent();
                } else {
                    try {
                        Field myParentField = root.getClass().getDeclaredField("myParent");
                        myParentField.setAccessible(true);
                        frame = (JFrame) myParentField.get(root);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (frame != null) {
                    hiddenHandle = new Panel();
                    hiddenHandle.setVisible(false);
                    hiddenHandle.setLocation(400, 400);
                    frame.add(hiddenHandle, BorderLayout.CENTER);
                }
            }
            if (hiddenHandle != null) {
                Pointer pointer = getComponentPointer(hiddenHandle);
                conEmuControl.setParentHWND(pointer);
            }
        }
    }

}
