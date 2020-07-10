package org.iceterm;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class LocalTerminalCustomizer {
    public static ExtensionPointName<LocalTerminalCustomizer> EP_NAME = ExtensionPointName.create("iceterm.localTerminalCustomizer");

    public String[] customizeCommandAndEnvironment(Project project, String[] command, Map<String, String> envs) {
        return command;
    }

    @Nullable
    public UnnamedConfigurable getConfigurable(Project project) {
        return null;
    }

    @Nullable
    protected String getDefaultFolder(Project project) {
        return null;
    }
}
