package org.iceterm;

import com.intellij.ide.actions.ToolWindowViewModeAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.containers.hash.HashMap;
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
import java.lang.reflect.Method;
import java.util.Map;

public class IceTermView {

    private IceTermToolWindow iceTermToolWindowPanel;
    private Project myProject;
    private ToolWindowImpl myToolWindow;
    private ConEmuStateChangedListener conEmuStateChangedListener;
    private boolean focusing;

    public ConEmuControl getConEmuControl() {
        return conEmuControl;
    }

    private ConEmuControl conEmuControl;
    private Panel hiddenHandle;
    private JFrame frame;
    private WindowInfoImpl windowInfo;
    private Map<String, WindowInfoImpl> mySameDockWindows = new HashMap<>();
    private ConEmuStartInfo startinfo;

    public IceTermView(@NotNull Project project) {
        myProject = project;
        iceTermToolWindowPanel = new IceTermToolWindow(project);
    }

    void initToolWindow(@NotNull ToolWindow toolWindow) {
        if (myToolWindow != null) {
            return;
        }

        myToolWindow = (ToolWindowImpl) toolWindow;

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
            window.show(null);
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
        Frame[] frames = IdeFrameImpl.getFrames();
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] instanceof JFrame && frames[i] instanceof IdeFrameImpl) {
                IdeFrameImpl frame = (IdeFrameImpl) frames[i];
                if (frame.getProject() == null) {
                    continue;
                }
                if (StringUtils.equals(frame.getProject().getName(), myProject.getName())) {
                    return frame;
                }

            }
        }
        return null;
    }

    private void createTerminalContent(ToolWindowImpl toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(iceTermToolWindowPanel.getContent(), "", false);
        final ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);

        createConEmuControl();
        storeState();
        addListeners();
    }

    private ConEmuControl createConEmuControl() {
        this.startinfo = new ConEmuStartInfo(myProject);

        conEmuControl = new ConEmuControl(startinfo);
        conEmuControl.setMinimumSize(new Dimension(400, 400));
        iceTermToolWindowPanel.getContent().add(conEmuControl);
        Color background = iceTermToolWindowPanel.getContent().getParent().getBackground();
        iceTermToolWindowPanel.getContent().setBackground(background);
        conEmuControl.setBackground(background);
        conEmuControl.addStateChangedListener(conEmuStateChangedListener);
        return conEmuControl;
    }

    private void storeState() {
        this.windowInfo = this.getWindowInfo(this.myToolWindow.getId()).copy();
        this.windowInfo.setType(getToolWindowType());
        mySameDockWindows = new HashMap<>();
        this.getSameDockWindows().forEach((key, value) -> mySameDockWindows.put(key, value));
    }

    private void addListeners() {
        myProject.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void stateChanged() {
                if (windowInfoChanged()) {
                    storeState();
                    saveTempHwnd();
                }
            }
        });

        myProject.getMessageBus().connect().subscribe(AnActionListener.TOPIC, new AnActionListener() {
            @Override
            public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, AnActionEvent event) {
                if (action instanceof ToolWindowViewModeAction) {
                    String activeToolWindowId = myToolWindow.getToolWindowManager().getActiveToolWindowId();
                    if (activeToolWindowId == null || !activeToolWindowId.equals(myToolWindow.getId())) {
                        return;
                    }
                    try {
                        Field viewMode = action.getClass().getDeclaredField("myMode");
                        viewMode.setAccessible(true);
                        ToolWindowViewModeAction.ViewMode myMode = (ToolWindowViewModeAction.ViewMode) viewMode.get(action);
                        if (myMode.equals(ToolWindowViewModeAction.ViewMode.Float)) {
                            saveTempHwnd();
                        }
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        AWTEventListener listener = event -> {
            if (conEmuControl == null) {
                return;
            }

            if (event.getID() == 1005) {
                FocusEvent focusEvent = (FocusEvent) event;
                if (isInToolWindow(focusEvent.getSource()) && !isInToolWindow(focusEvent.getOppositeComponent())) {
                    if (!focusEvent.isTemporary() && myToolWindow != null && (myToolWindow.isAutoHide() || myToolWindow.getType() == ToolWindowType.SLIDING)) {
                        saveTempHwnd();
                    }
                }
            }

            if (event.getID() == 1004) {
                FocusEvent focusEvent = (FocusEvent) event;
                if (isInToolWindow(focusEvent.getSource())) {
                    JComponent parent = (JComponent) conEmuControl.getParent();
                    parent.getRootPane().getParent().setVisible(true);
                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(20);
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
                String activeToolWindowId = myToolWindow.getToolWindowManager().getActiveToolWindowId();
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

    private ToolWindowType getToolWindowType() {
        String id = myToolWindow.getId();
        ToolWindowType type = null;
        try {
            Field myId2InternalDecoratorField = myToolWindow.getToolWindowManager().getClass().getDeclaredField("myId2InternalDecorator");
            myId2InternalDecoratorField.setAccessible(true);
            Map<String, InternalDecorator> myId2InternalDecorator = (Map<String, InternalDecorator>) myId2InternalDecoratorField.get(myToolWindow.getToolWindowManager());
            Field myId2FloatingDecoratorField = myToolWindow.getToolWindowManager().getClass().getDeclaredField("myId2FloatingDecorator");
            myId2FloatingDecoratorField.setAccessible(true);
            Map<String, InternalDecorator> myId2FloatingDecorator = (Map<String, InternalDecorator>) myId2FloatingDecoratorField.get(myToolWindow.getToolWindowManager());
            Field myId2WindowedDecoratorField = myToolWindow.getToolWindowManager().getClass().getDeclaredField("myId2WindowedDecorator");
            myId2WindowedDecoratorField.setAccessible(true);
            Map<String, InternalDecorator> myId2WindowedDecorator = (Map<String, InternalDecorator>) myId2WindowedDecoratorField.get(myToolWindow.getToolWindowManager());
            if (myId2FloatingDecorator.containsKey(id)) {
                type = ToolWindowType.FLOATING;
            } else if (myId2WindowedDecorator.containsKey(id)) {
                type = ToolWindowType.WINDOWED;
            } else if (myId2InternalDecorator.containsKey(id)) {
                type = ToolWindowType.DOCKED;
            } else {
                type = ToolWindowType.SLIDING;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return type;
    }

    private Map<String, WindowInfoImpl> getSameDockWindows() {
        Map<String, WindowInfoImpl> sameDockWindows = new HashMap<>();
        try {
            Field myId2InternalDecoratorField = myToolWindow.getToolWindowManager().getClass().getDeclaredField("myId2InternalDecorator");
            myId2InternalDecoratorField.setAccessible(true);
            Map<String, InternalDecorator> myId2InternalDecorator = (Map<String, InternalDecorator>) myId2InternalDecoratorField.get(myToolWindow.getToolWindowManager());
            myId2InternalDecorator.forEach((key, value) -> {
                WindowInfoImpl windowInfo = getWindowInfo(key);
                if (windowInfo.getAnchor() == this.windowInfo.getAnchor() && windowInfo.isDocked() && windowInfo.isVisible()) {
                    sameDockWindows.put(key, windowInfo);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sameDockWindows;
    }

    private boolean windowInfoChanged() {
        WindowInfoImpl newWindowInfo = this.getWindowInfo(myToolWindow.getId());
        boolean changed = false;
        changed |= newWindowInfo.isVisible() != windowInfo.isVisible();
        changed |= newWindowInfo.isAutoHide() != windowInfo.isAutoHide();
        changed |= newWindowInfo.isMaximized() != windowInfo.isMaximized();
        changed |= newWindowInfo.getAnchor() != windowInfo.getAnchor();
        changed |= newWindowInfo.getType() != windowInfo.getType();

        changed |= !this.getSameDockWindows().keySet().equals(this.mySameDockWindows.keySet()) && this.windowInfo.isDocked();
        return changed;
    }

    private void saveTempHwnd() {
        if (conEmuControl.getSession() != null) {
            Component root = SwingUtilities.getRoot(myToolWindow.getComponent());
            if (root != null && frame == null) {
                if (root instanceof IdeFrameImpl) {
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
                conEmuControl.setParentHWND(getComponentPointer(hiddenHandle));
            }
        }
    }

    private WindowInfoImpl getWindowInfo(String id) {
        WindowInfoImpl returnValue = null;
        try {
            Method method = myToolWindow.getToolWindowManager().getClass().getDeclaredMethod("getRegisteredInfoOrLogError", String.class);
            method.setAccessible(true);
            returnValue = (WindowInfoImpl) method.invoke(myToolWindow.getToolWindowManager(), id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }

}
