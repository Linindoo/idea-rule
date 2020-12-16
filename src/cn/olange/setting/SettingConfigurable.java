package cn.olange.setting;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingConfigurable implements SearchableConfigurable {
    public static final String DISPLAY_NAME = "any rule";

    private SettingUI mainPanel;
    @NotNull
    @Override
    public String getId() {
        return "any.rule";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return DISPLAY_NAME;
    }


    @Nullable
    @Override
    public JComponent createComponent() {
        mainPanel = new SettingUI();
        return mainPanel.getComponent();
    }

    @Override
    public boolean isModified() {
        return mainPanel.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        mainPanel.apply();
    }

    @Override
    public void reset() {
        mainPanel.reset();
    }

    @Override
    public void disposeUIResources() {
//        mainPanel.disposeUIResources();
        mainPanel = null;
    }

}
