package org.iceterm;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginInstaller;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class IceTermPostStartupActivity implements StartupActivity {
    public static final Logger log = Logger.getLogger("PluginPostStartupActivity");

    @Override
    public void runActivity(@NotNull Project project) {
        PluginInstaller.addStateListener(new PluginStateListener() {
            @Override
            public void install(@NotNull IdeaPluginDescriptor ideaPluginDescriptor) {
//                try {
//                    ConEmuStartInfo.extractBinaries();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }

            @Override
            public void uninstall(@NotNull IdeaPluginDescriptor ideaPluginDescriptor) {
//                ConEmuControl.terminate();
//                File bin = new File(ConEmuStartInfo.binFolder);
//                if(bin.exists()) {
//                    bin.delete();
//                }
            }
        });
    }
}
