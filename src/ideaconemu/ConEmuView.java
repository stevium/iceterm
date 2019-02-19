package ideaconemu;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.StripeButton;
import com.intellij.openapi.wm.impl.ToolWindowImpl;
import com.intellij.openapi.wm.impl.WindowInfoImpl;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.uiDesigner.core.GridLayoutManager;
import conemu.jni.GuiMacroExecutor_N;
import conemu.util.WinApi;
import org.jetbrains.annotations.NotNull;
import sun.awt.windows.WComponentPeer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.peer.ContainerPeer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ConEmuView {
    private Project myProject;
    private ToolWindowImpl myToolWindow;
    Label conEmuPanel = new Label();
    Label conEmuPanel2 = new Label();
    WComponentPeer peer;
    long hwnd;
    private ContainerPeer rootPeer;
    private JPanel rootFrame;
    private boolean visible = true;
    private WComponentPeer peer2;
    private long hwnd2;
    private Process process;

    String asLibrary = "C:\\Users\\Milos\\RiderProjects\\ConEmu\\Release\\ConEmu\\ConEmuCD64.dll";

    public ConEmuView(@NotNull Project project) {
        myProject = project;
    }

    void initToolWindow(@NotNull ToolWindow toolWindow) {
        if (myToolWindow != null) {
            return;
        }

        myToolWindow = (ToolWindowImpl) toolWindow;

        if (myToolWindow.getContentManager().getContentCount() == 0) {
            createNewSession();
        }
    }

    public static ConEmuView getInstance(@NotNull Project project) {
        return project.getComponent(ConEmuView.class);
    }

    public void createNewSession() {
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(ConEmuToolWindowFactory.TOOL_WINDOW_ID);
        if (window != null && window.isAvailable()) {
            // ensure TerminalToolWindowFactory.createToolWindowContent gets called
//            ((ToolWindowImpl)window).ensureContentInitialized();
            createTerminalContent(myToolWindow);
            window.activate(null);
        }
    }

    private Content createTerminalContent(ToolWindowImpl toolWindow) {
        ConEmuToolWindow ideaConEmuToolWindow = new ConEmuToolWindow();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(ideaConEmuToolWindow.getContent(), "", false);
        final ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContent(content);

        ideaConEmuToolWindow.jpanel.add(conEmuPanel);
        ideaConEmuToolWindow.jpanel2.add(conEmuPanel2);

        toolWindow.getToolWindowManager().addToolWindowManagerListener(new ToolWindowManagerListener() {
            @Override
            public void toolWindowRegistered(@NotNull String id) {
                System.out.println("toolWindowRegistered: " + id);
            }

            @Override
            public void toolWindowUnregistered(@NotNull String id, @NotNull ToolWindow toolWindow) {
                System.out.println("toolWindowUnregistered: " + id);
            }

            @Override
            public void stateChanged() {
            }
        });

        myProject.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void stateChanged() {
                if (toolWindow.isVisible()) {
                } else {

                }
            }
        });

        ideaConEmuToolWindow.button2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                addStripeButtonListener(toolWindow);
//                JNIHelper jniHelper = new JNIHelper();
//                JFrame frame = (JFrame) SwingUtilities.getRoot(myToolWindow.getComponent());
//                Long parentHandle = ((WComponentPeer)frame.getPeer()).getHWnd();
//                jniHelper.setParent(hwnd, parentHandle);
//                frame.add(conEmuPanel);

//                ideaConEmuToolWindow.button2.setText(Integer.toString(handle2));

//                String asLibrary = "C:\\Users\\Milos\\RiderProjects\\conemu-inside\\ConEmuInside\\bin\\Debug\\ConEmu\\ConEmuCD64.dll";
                GuiMacroExecutor_N executor_n = new GuiMacroExecutor_N();
                long conEmuCDdll = executor_n.loadConEmuDll(asLibrary);
                System.out.println("ConEmuDll loaded on address: " + conEmuCDdll);
                long guiMacroFn = executor_n.initGuiMacroFn();
                System.out.println("guiMacroFn loaded on address: " + guiMacroFn);
                int processId = WinApi.Helpers.getProcessId(process);
                executor_n.executeInProcess(String.valueOf(processId), "SetParentHWND " + hwnd2);

            }
        });

        ideaConEmuToolWindow.button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                peer = ((WComponentPeer) conEmuPanel.getPeer());
                hwnd = peer.getHWnd();

                peer2 = ((WComponentPeer) conEmuPanel2.getPeer());
                hwnd2 = peer2.getHWnd();

                ideaConEmuToolWindow.button1.setText(Long.toHexString(hwnd));
                ideaConEmuToolWindow.button2.setText(Long.toHexString(hwnd2));


                runConEmuInside(ideaConEmuToolWindow, hwnd);
            }
        });

        ideaConEmuToolWindow.button3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                hwnd = ((WComponentPeer)SwingUtilities.getRoot(myToolWindow.getComponent()).getPeer()).getHWnd();
//                ideaConEmuToolWindow.button3.setText(Long.toHexString(hwnd));
//                runConEmuInside(ideaConEmuToolWindow, hwnd);

                GuiMacroExecutor_N executor_n = new GuiMacroExecutor_N();
                long conEmuCDdll = executor_n.loadConEmuDll(asLibrary);
                System.out.println("ConEmuDll loaded on address: " + conEmuCDdll);
                long guiMacroFn = executor_n.initGuiMacroFn();
                System.out.println("guiMacroFn loaded on address: " + guiMacroFn);
                int processId = WinApi.Helpers.getProcessId(process);
                executor_n.executeInProcess(String.valueOf(processId), "SetParentHWND " + hwnd);

//                ideaConEmuToolWindow.jpanel.remove(conEmuPanel);
            }
        });

        return content;
    }

    private void addStripeButtonListener(ToolWindowImpl toolWindow) {
        try {
            Field myId2StripeButtonField = toolWindow.getToolWindowManager().getClass().getDeclaredField("myId2StripeButton");
            myId2StripeButtonField.setAccessible(true);
            Map<String, StripeButton> myId2StripeButton = (Map<String, StripeButton>)myId2StripeButtonField.get(toolWindow.getToolWindowManager());
            StripeButton stripeButton = myId2StripeButton.get(toolWindow.getId());
            stripeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Method method = myToolWindow.getToolWindowManager().getClass().getDeclaredMethod("getRegisteredInfoOrLogError", String.class);
                        method.setAccessible(true);
                        WindowInfoImpl returnValue = (WindowInfoImpl) method.invoke(myToolWindow.getToolWindowManager(), myToolWindow.getId());
                        if(returnValue.isFloating()) {
                            if (myToolWindow.isVisible()) {
                                returnValue.setVisible(false);
                                myToolWindow.getComponent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().setVisible(false);
                                myToolWindow.getComponent().getParent().getParent().getParent().setVisible(false);
                            } else {
                                returnValue.setVisible(true);
                                myToolWindow.getComponent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().setVisible(true);
                                myToolWindow.getComponent().getParent().getParent().getParent().setVisible(true);
                            }
                        } else if(returnValue.isDocked()) {
                            if (myToolWindow.isVisible()) {
                                returnValue.setVisible(false);
//                                myToolWindow.getComponent().getParent().getParent().getParent().getParent().setSize(new Dimension(0,0));
                                myToolWindow.getComponent().getParent().getParent().getParent().getParent().getParent().setVisible(false);
                            } else {
                                returnValue.setVisible(true);
//                                myToolWindow.getComponent().getParent().getParent().getParent().getParent().setSize(new Dimension(400,400));
                                myToolWindow.getComponent().getParent().getParent().getParent().getParent().getParent().setVisible(true);
                            }
                        }
                    } catch (NoSuchMethodException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    }
                }
            });
        } catch (NoSuchFieldException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }
    }

    private ConEmuToolWindow runConEmuInside(ConEmuToolWindow ideaConEmuToolWindow, Long handle) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    process = new ProcessBuilder(
                            "C:\\Users\\Milos\\RiderProjects\\ConEmu\\Release\\ConEmu.exe",
                            "/NoKeyHooks",
                            "/Single",
                            "/InsideWnd", "0x" + Long.toHexString(handle),
                            "/LoadCfgFile", "C:\\Users\\Milos\\RiderProjects\\conemu-inside\\ConEmuInside\\bin\\Debug\\ConEmu.xml"
                    ).start();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }, 200);
        return ideaConEmuToolWindow;
    }
}
