package iceterm;

import com.google.common.collect.Lists;
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Arrays;

public class IceTermSettingsPanel {
    private JPanel myProjectSettingsPanel;
    private TextFieldWithBrowseButton myStartDirectoryField;
    private EnvironmentVariablesTextFieldWithBrowseButton myEnvVarField;
    private JPanel myGlobalSettingsPanel;
    private JTextField myTabNameTextField;
    private TextFieldWithBrowseButton myShellPathField;
    private JBCheckBox mySoundBellCheckBox;
    private JPanel myConfigurablesPanel;
    private JPanel myWholePanel;

    private IceTermOptionsProvider myOptionsProvider;
    private IceTermProjectOptionsProvider myProjectOptionsProvider;

    private final java.util.List<UnnamedConfigurable> myConfigurables = Lists.newArrayList();

    public JComponent createPanel(@NotNull IceTermOptionsProvider provider, @NotNull IceTermProjectOptionsProvider projectOptionsProvider) {
        myOptionsProvider = provider;
        myProjectOptionsProvider = projectOptionsProvider;

        myProjectSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Project settings"));
        myGlobalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Application settings"));

        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);

        myShellPathField.addBrowseFolderListener(
                "",
                "Shell executable path",
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        fileChooserDescriptor = new FileChooserDescriptor(false, true, false, false, false, false);

        myStartDirectoryField.addBrowseFolderListener(
                "",
                "Starting directory",
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        myShellPathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                myShellPathField
                        .getTextField().setForeground(StringUtil.equals(myShellPathField.getText(), myProjectOptionsProvider.getDefaultShellPath()) ?
                        getDefaultValueColor() : getChangedValueColor());
            }
        });

        myStartDirectoryField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                myStartDirectoryField
                        .getTextField()
                        .setForeground(StringUtil.equals(myStartDirectoryField.getText(), myProjectOptionsProvider.getDefaultStartingDirectory()) ?
                                getDefaultValueColor() : getChangedValueColor());
            }
        });

        return myWholePanel;
    }

    public boolean isModified() {
        return !Comparing.equal(myShellPathField.getText(), myOptionsProvider.getShellPath())
                || !Comparing.equal(myStartDirectoryField.getText(), StringUtil.notNullize(myProjectOptionsProvider.getStartingDirectory()))
                || !Comparing.equal(myTabNameTextField.getText(), myOptionsProvider.getTabName())
                || (mySoundBellCheckBox.isSelected() != myOptionsProvider.audibleBell())
                || myConfigurables.stream().anyMatch(c -> c.isModified())
                || !Comparing.equal(myEnvVarField.getData(), myOptionsProvider.getEnvData());
    }

    public void apply() {
        myProjectOptionsProvider.setStartingDirectory(myStartDirectoryField.getText());
        myOptionsProvider.setShellPath(myShellPathField.getText());
        myOptionsProvider.setTabName(myTabNameTextField.getText());
        myOptionsProvider.setSoundBell(mySoundBellCheckBox.isSelected());
        myConfigurables.forEach(c -> {
            try {
                c.apply();
            }
            catch (ConfigurationException e) {
                //pass
            }
        });
        myOptionsProvider.setEnvData(myEnvVarField.getData());
    }

    public void reset() {
        myShellPathField.setText(myOptionsProvider.getShellPath());
        myStartDirectoryField.setText(myProjectOptionsProvider.getStartingDirectory());
        myTabNameTextField.setText(myOptionsProvider.getTabName());
        mySoundBellCheckBox.setSelected(myOptionsProvider.audibleBell());
        myConfigurables.forEach(c -> c.reset());
        myEnvVarField.setData(myOptionsProvider.getEnvData());
    }
    public Color getDefaultValueColor() {
        return findColorByKey("TextField.inactiveForeground", "nimbusDisabledText");
    }

    public Color getChangedValueColor() {
        return findColorByKey("TextField.foreground");
    }

    @NotNull
    private static Color findColorByKey(String... colorKeys) {
        Color c = null;
        for (String key : colorKeys) {
            c = UIManager.getColor(key);
            if (c != null) {
                break;
            }
        }

        assert c != null : "Can't find color for keys " + Arrays.toString(colorKeys);
        return c;
    }

}
