package org.iceterm;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.containers.hash.HashMap;
import org.iceterm.integration.ConEmuControl;
import org.iceterm.integration.ConEmuStartInfo;
import org.iceterm.integration.States;
import org.iceterm.jni.GuiMacroExecutor_N;
import org.iceterm.util.WinApi;
import org.jetbrains.annotations.NotNull;
import sun.awt.windows.WComponentPeer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class IceTermView {

    private final String conEmuExe = "C:\\Users\\Milos\\RiderProjects\\ConEmu\\Release\\ConEmu64.exe";
    private final String configFile = "C:\\Users\\Milos\\RiderProjects\\conemu-inside\\ConEmuInside\\bin\\Debug\\ConEmu.xml";
    private final String conEmuCD = "C:\\Users\\Milos\\RiderProjects\\ConEmu\\Release\\ConEmu\\ConEmuCD64.dll";

    private IceTermToolWindow ideaIceTermToolWindow;
    private Project myProject;
    private ToolWindowImpl myToolWindow;
    private GuiMacroExecutor_N executor_n;
    private ConEmuControl conEmuControl;
    private Panel tempBackup;
    private JFrame frame;
    private WindowInfoImpl windowInfo;
    private Map<String, WindowInfoImpl> mySameDockWindows = new HashMap<>();

    public IceTermView(@NotNull Project project) {
        myProject = project;
        ideaIceTermToolWindow = new IceTermToolWindow();
    }

    void initToolWindow(@NotNull ToolWindow toolWindow) {
        if (myToolWindow != null) {
            return;
        }

        myToolWindow = (ToolWindowImpl) toolWindow;

        if (myToolWindow.getContentManager().getContentCount() == 0) {
            createNewSession();
        }

        initGuiMacroExecutor();

    }

    public static IceTermView getInstance(@NotNull Project project) {
        return project.getComponent(IceTermView.class);
    }

    public void createNewSession() {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(IceTermToolWindowFactory.TOOL_WINDOW_ID);
        if (window != null && window.isAvailable()) {
            createTerminalContent(myToolWindow);
            window.activate(null);
            conEmuControl.addStateChangedListener(new ConEmuControl.StateChagedListener() {
                @Override
                public void stateChanged() {
                    States state = conEmuControl.getState();
                    if(state == States.ConsoleEmulatorWithConsoleProcess) {
                        Process process = conEmuControl.getSession().getProcess();
                        executor_n.runPipeServer();
                        executor_n.runPipeClient(WinApi.Helpers.getProcessId(process));
                    }
                }
            });
        }
    }

    private Content createTerminalContent(ToolWindowImpl toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(ideaIceTermToolWindow.getContent(), "", false);
        final ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);

        createConEmuControl();
        storeState();
        addListeners();

        return content;
    }

    private ConEmuControl createConEmuControl() {
        ideaIceTermToolWindow.jpanel.setBackground(Color.darkGray);
        ConEmuStartInfo startinfo = new ConEmuStartInfo();
        StringBuilder sbText = new StringBuilder();
        startinfo.setConEmuExecutablePath(conEmuExe);
        startinfo.setConEmuConsoleServerExecutablePath(conEmuCD);
//        startinfo.setConsoleProcessCommandLine("ping 8.8.8.8");
        startinfo.setLogLevel(ConEmuStartInfo.LogLevels.Basic);
        startinfo.setAnsiStreamChunkReceivedEventSink((source, event) -> {
            sbText.append(event.GetMbcsText());
        });

        conEmuControl = new ConEmuControl(startinfo);
        conEmuControl.setMinimumSize(new Dimension(400, 400));
        ideaIceTermToolWindow.jpanel.add(conEmuControl);
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
                if (action.getClass().getName().contains(InternalDecorator.TOGGLE_FLOATING_MODE_ACTION_ID)
                && myToolWindow.getToolWindowManager().getActiveToolWindowId() == myToolWindow.getId()) {
                    saveTempHwnd();
                }
            }
        });

        AWTEventListener listener = event -> {
                if (event.getID() == 1005) {
                    FocusEvent focusEvent = (FocusEvent) event;
                    if (isInActiveToolWindow(focusEvent.getSource()) && !isInActiveToolWindow(focusEvent.getOppositeComponent())) {
                        if (!focusEvent.isTemporary() && myToolWindow != null && (myToolWindow.isAutoHide() || myToolWindow.getType() == ToolWindowType.SLIDING)) {
                            saveTempHwnd();
                        }
                    }
                }

        };
        AWTEventListener internalEventLostListener = Toolkit.getDefaultToolkit().getAWTEventListeners()[2];
        Toolkit.getDefaultToolkit().removeAWTEventListener(internalEventLostListener);
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, 28L);
        Toolkit.getDefaultToolkit().addAWTEventListener(internalEventLostListener, 28L);
    }

    public boolean isInActiveToolWindow(Object component) {
        JComponent source = component instanceof JComponent ? (JComponent)component : null;
//        ToolWindow activeToolWindow = ToolWindowManager.getActiveToolWindow();
        ToolWindow activeToolWindow = this.myToolWindow;
        if (activeToolWindow == null) {
            return false;
        } else {
            JComponent activeToolWindowComponent = activeToolWindow.getComponent();
            if (activeToolWindowComponent != null) {
                while(source != null && source != activeToolWindowComponent) {
                    source = source.getParent() != null && source.getParent() instanceof JComponent ? (JComponent)source.getParent() : null;
                }
            }

            return source != null;
        }
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
                    tempBackup = new Panel();
                    tempBackup.setVisible(false);
                    tempBackup.setLocation(400, 400);
                    frame.add(tempBackup, BorderLayout.CENTER);
                }
            }
            conEmuControl.setParentHWND(((WComponentPeer) tempBackup.getPeer()).getHWnd());
        }
    }

    private GuiMacroExecutor_N initGuiMacroExecutor() {
        this.executor_n = new GuiMacroExecutor_N();
        long conEmuCDdll = executor_n.loadConEmuDll(conEmuCD);
        System.out.println("ConEmuDll loaded on address: " + conEmuCDdll);
        long guiMacroFn = executor_n.initGuiMacroFn();
        System.out.println("guiMacroFn loaded on address: " + guiMacroFn);
        return executor_n;
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
