package org.iceterm;

import com.google.common.collect.Lists;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Arrays;

public class IceTermSettingsPanel {
    private JPanel myProjectSettingsPanel;
    private TextFieldWithBrowseButton myStartDirectoryField;
    private JPanel myGlobalSettingsPanel;
    private TextFieldWithBrowseButton myConEmuPathField;
    private JPanel myWholePanel;
    private JTextField myShellTaskField;
    private ShortcutTextField myEscapeKeyField;
    private TextFieldWithBrowseButton myConEmuXmlPathField;

    private IceTermOptionsProvider myOptionsProvider;
    private IceTermProjectOptionsProvider myProjectOptionsProvider;

    private final java.util.List<UnnamedConfigurable> myConfigurables = Lists.newArrayList();

    public JComponent createPanel(@NotNull IceTermOptionsProvider provider, @NotNull IceTermProjectOptionsProvider projectOptionsProvider) {
        myOptionsProvider = provider;
        myProjectOptionsProvider = projectOptionsProvider;
        myProjectSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Project settings"));
        myGlobalSettingsPanel.setBorder(IdeBorderFactory.createTitledBorder("Application settings"));

        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, false, false, false, false, false);

        myConEmuPathField.addBrowseFolderListener(
                "",
                "Shell executable path",
                null,
                fileChooserDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);

        myConEmuPathField.addBrowseFolderListener(
                "",
                "ConEmu.xml file path",
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

        myConEmuPathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                myConEmuPathField
                        .getTextField().setForeground(StringUtil.equals(myConEmuPathField.getText(), myOptionsProvider.defaultConEmuPath()) ?
                        getDefaultValueColor() : getChangedValueColor());
            }
        });

        myConEmuXmlPathField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                myConEmuXmlPathField
                        .getTextField().setForeground(StringUtil.equals(myConEmuXmlPathField.getText(), myOptionsProvider.defaultConEmuXmlPath()) ?
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

        myShellTaskField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                myShellTaskField
                        .setForeground(StringUtil.equals(myShellTaskField.getText(), myOptionsProvider.defaultShellTask()) ?
                        getDefaultValueColor() : getChangedValueColor());
            }
        });

       myEscapeKeyField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                myEscapeKeyField
                        .setForeground(myOptionsProvider.defaultEscapeKey().equals(myEscapeKeyField.getKeyStroke()) ?
                        getDefaultValueColor() : getChangedValueColor());
            }
        });

        return myWholePanel;
    }

    public boolean isModified() {
        return !Comparing.equal(myConEmuPathField.getText(), myOptionsProvider.getConEmuPath())
                || !Comparing.equal(myConEmuXmlPathField.getText(), myOptionsProvider.getConEmuXmlPath())
                || !Comparing.equal(myStartDirectoryField.getText(), StringUtil.notNullize(myProjectOptionsProvider.getStartingDirectory()))
                || !myOptionsProvider.getEscapeKey().equals(myEscapeKeyField.getKeyStroke())
                || !Comparing.equal(myShellTaskField.getText(), myOptionsProvider.getShellTask())
                || myConfigurables.stream().anyMatch(c -> c.isModified());
    }

    public void apply() {
        myProjectOptionsProvider.setStartingDirectory(myStartDirectoryField.getText());
        myOptionsProvider.setConEmuPath(myConEmuPathField.getText());
        myOptionsProvider.setConEmuXmlPath(myConEmuXmlPathField.getText());
        myOptionsProvider.setEscapeKey(myEscapeKeyField.getKeyStroke());
        myOptionsProvider.setShellTask(myShellTaskField.getText());
        myConfigurables.forEach(c -> {
            try {
                c.apply();
            }
            catch (ConfigurationException e) {
                //pass
            }
        });
    }

    public void reset() {
        myConEmuPathField.setText(myOptionsProvider.getConEmuPath());
        myConEmuXmlPathField.setText(myOptionsProvider.getConEmuXmlPath());
        myStartDirectoryField.setText(myProjectOptionsProvider.getStartingDirectory());
        myEscapeKeyField.setKeyStroke(myOptionsProvider.getEscapeKey());
        myShellTaskField.setText(myOptionsProvider.getShellTask());
        myConfigurables.forEach(c -> c.reset());
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

    private void createUIComponents() {
        myEscapeKeyField =  new ShortcutTextField(true);
    }
}
