package ideaconemu;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;
import sun.awt.windows.WComponentPeer;

import java.awt.*;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class ConEmuToolWindowFactory implements ToolWindowFactory {
    public static final String TOOL_WINDOW_ID = "IdeaConEmu";

    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        ConEmuView terminalView = ConEmuView.getInstance(project);
        terminalView.initToolWindow(toolWindow);

//        ConEmuToolWindow ideaConEmuToolWindow = new ConEmuToolWindow();
//        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
//        Content content = contentFactory.createContent(ideaConEmuToolWindow.getContent(), "", false);
//        toolWindow.getContentManager().addContent(content);
//
//        Canvas conEmuPanel = new Canvas();
//        conEmuPanel.setBackground(Color.GREEN);
//        ideaConEmuToolWindow.myToolWindowContent.add(conEmuPanel, BorderLayout.CENTER);
//
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                long hwnd = ((WComponentPeer)conEmuPanel.getPeer()).getHWnd();
//                try {
//                    new ProcessBuilder(
//                            "C:\\Users\\Milos\\RiderProjects\\conemu-inside\\ConEmuInside\\bin\\Debug\\ConEmu\\ConEmu.exe",
//                            "/NoKeyHooks",
//                            "/InsideWnd", "0x" + Long.toHexString(hwnd),
//                            "/detached"
//                    ).start();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
//            }
//        }, 100);
    }
}

