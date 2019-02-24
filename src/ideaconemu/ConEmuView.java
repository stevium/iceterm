package ideaconemu;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import conemu.ConEmuControl;
import conemu.ConEmuStartInfo;
import conemu.jni.GuiMacroExecutor_N;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import sun.awt.windows.WComponentPeer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.lang.reflect.Field;

public class ConEmuView {

    private final String conEmuExe = "C:\\Users\\Milos\\RiderProjects\\ConEmu\\Release\\ConEmu.exe";
    private final String configFile = "C:\\Users\\Milos\\RiderProjects\\conemu-inside\\ConEmuInside\\bin\\Debug\\ConEmu.xml";
    private final String conEmuCD = "C:\\Users\\Milos\\RiderProjects\\ConEmu\\Release\\ConEmu\\ConEmuCD64.dll";

    private ConEmuToolWindow ideaConEmuToolWindow;
    private Project myProject;
    private ToolWindowImpl myToolWindow;
    private GuiMacroExecutor_N executor_n;
    private ConEmuControl conEmuControl;
    private Panel tempBackup;
    private JFrame frame;
    private Element state;

    public ConEmuView(@NotNull Project project) {
        myProject = project;
        ideaConEmuToolWindow = new ConEmuToolWindow();
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

    public static ConEmuView getInstance(@NotNull Project project) {
        return project.getComponent(ConEmuView.class);
    }

    public void createNewSession() {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(ConEmuToolWindowFactory.TOOL_WINDOW_ID);
        if (window != null && window.isAvailable()) {
            createTerminalContent(myToolWindow);
            window.activate(null);
        }
    }

    private Content createTerminalContent(ToolWindowImpl toolWindow) {
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(ideaConEmuToolWindow.getContent(), "", false);
        final ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);

        createConEmuControl();
        state = myToolWindow.getToolWindowManager().getState();

        myProject.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void stateChanged() {
                Element newState = myToolWindow.getToolWindowManager().getState();
                if (isChanged(newState)) {
                    ConEmuView.this.state = newState;
                    saveTempHwnd();
                }
            }
        });

        myProject.getMessageBus().connect().subscribe(AnActionListener.TOPIC, new AnActionListener() {
            @Override
            public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, AnActionEvent event) {
                if (action.getClass().getName().contains(InternalDecorator.TOGGLE_FLOATING_MODE_ACTION_ID)) {
                    saveTempHwnd();
                }
            }
        });

        ideaConEmuToolWindow.button1.addActionListener(e -> {
        });

        return content;
    }

    private boolean isChanged(Element newState) {
        Element frame = state.getChild("frame");
        Attribute x = frame.getAttribute("x");
        Attribute y = frame.getAttribute("y");
        Attribute width = frame.getAttribute("width");
        Attribute height = frame.getAttribute("height");
        Attribute extendedState = frame.getAttribute("extended-state");

        Element newFrame = newState.getChild("frame");
        Attribute newX = frame.getAttribute("x");
        Attribute newY = frame.getAttribute("y");
        Attribute newWidth = frame.getAttribute("width");
        Attribute newHeight = frame.getAttribute("height");
        Attribute newExtendedState = frame.getAttribute("extended-state");

        return x.getValue().equals(newX.getValue()) ||
                y.getValue().equals(newY.getValue()) ||
                width.getValue().equals(newWidth.getValue()) ||
                height.getValue().equals(newHeight.getValue()) ||
                extendedState.getValue().equals(newExtendedState.getValue());
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

    private ConEmuControl createConEmuControl() {
        ideaConEmuToolWindow.jpanel.setBackground(Color.darkGray);
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
        ideaConEmuToolWindow.jpanel.add(conEmuControl);
        return conEmuControl;
    }

    private GuiMacroExecutor_N initGuiMacroExecutor() {
        this.executor_n = new GuiMacroExecutor_N();
        long conEmuCDdll = executor_n.loadConEmuDll(conEmuCD);
        System.out.println("ConEmuDll loaded on address: " + conEmuCDdll);
        long guiMacroFn = executor_n.initGuiMacroFn();
        System.out.println("guiMacroFn loaded on address: " + guiMacroFn);
        return executor_n;
    }

}
