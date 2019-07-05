package org.iceterm;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.iceterm.ceintegration.ConEmuStartInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class IceTermOptionsConfigurable implements SearchableConfigurable, Disposable {
    public static final String TERMINAL_SETTINGS_HELP_REFERENCE = "reference.settings.terminal";

    private IceTermSettingsPanel myPanel;
    private final IceTermOptionsProvider myOptionsProvider;
    private final IceTermProjectOptionsProvider myProjectOptionsProvider;

    public IceTermOptionsConfigurable(@NotNull Project project) {
        myOptionsProvider = IceTermOptionsProvider.getInstance();
        myProjectOptionsProvider = IceTermProjectOptionsProvider.getInstance(project);
    }

    @NotNull
    @Override
    public String getId() {
        return "terminal";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Terminal";
    }

    @Override
    public String getHelpTopic() {
        return TERMINAL_SETTINGS_HELP_REFERENCE;
    }

    @Override
    public JComponent createComponent() {
        myPanel = new IceTermSettingsPanel();
        return myPanel.createPanel(myOptionsProvider, myProjectOptionsProvider);
    }

    @Override
    public boolean isModified() {
        return myPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        myPanel.apply();
    }

    @Override
    public void reset() {
        myPanel.reset();
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(this);
    }

    @Override
    public void dispose() {
        myPanel = null;
    }
}

