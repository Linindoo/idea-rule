package cn.olange.setting;

import cn.olange.model.Config;
import cn.olange.service.RuleDataService;
import cn.olange.utils.HttpUtil;
import com.google.gson.JsonArray;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;

public class SettingUI {
    private JTextField remoteUrl;
    private JButton dataUpdate;
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JLabel helpbtn;
    private Editor dataEditor;
    private String rawFileContent = "";


    public SettingUI() {
        dataEditor = EditorFactory.getInstance().createEditor(EditorFactory.getInstance().createDocument(""), null, FileTypeManager.getInstance().getFileTypeByExtension("js"), false);
        EditorSettings templateHelpEditorSettings = dataEditor.getSettings();
        templateHelpEditorSettings.setAdditionalLinesCount(0);
        templateHelpEditorSettings.setAdditionalColumnsCount(0);
        templateHelpEditorSettings.setLineMarkerAreaShown(false);
        templateHelpEditorSettings.setLineNumbersShown(false);
        templateHelpEditorSettings.setVirtualSpace(false);
        JBScrollPane jbScrollPane = new JBScrollPane(dataEditor.getComponent());
        contentPanel.setLayout(new BorderLayout());
        contentPanel.add(jbScrollPane, BorderLayout.CENTER);
        this.helpbtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserUtil.browse("https://github.com/Linindoo/idea-rule");
            }
        });
        this.dataUpdate.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dataUpdate.setEnabled(false);
                ApplicationManager.getApplication().executeOnPooledThread(()->{
                    try {
                        String ret = HttpUtil.getJson(remoteUrl.getText());
                        UIUtil.invokeLaterIfNeeded(() -> {
                            WriteCommandAction.runWriteCommandAction(null, () -> {
                                dataEditor.getDocument().setText(ret);
                            });
                        });
                        UIUtil.invokeLaterIfNeeded(() -> {
                            dataUpdate.setEnabled(true);
                            JBPopupFactory.getInstance().createBalloonBuilder(new JLabel("数据已更新"))
                                    .setFillColor(contentPanel.getBackground())
                                    .setAnimationCycle(0)
                                    .setFadeoutTime(0)
                                    .setRequestFocus(true)
                                    .createBalloon()
                                    .show(new RelativePoint(contentPanel, new Point(10, 10)), Balloon.Position.above);
                        });

                    } catch (IOException ioException) {
                        UIUtil.invokeLaterIfNeeded(() -> {
                            JBPopupFactory.getInstance().createBalloonBuilder(new JLabel("获取失败，请检查地址是否正确或者网络是否可用"))
                                    .setFillColor(contentPanel.getBackground())
                                    .setAnimationCycle(0)
                                    .setFadeoutTime(0)
                                    .setRequestFocus(true)
                                    .createBalloon()
                                    .show(new RelativePoint(dataUpdate, new Point(10, 10)), Balloon.Position.below);
                        });
                    }
                });
            }
        });
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public boolean isModified() {
        Config config = RulePersistentConfig.getInstance().getState();
        return !Comparing.strEqual(config.getUpdateUrl(), remoteUrl.getText()) || !Comparing.strEqual(rawFileContent, dataEditor.getDocument().getText());
    }

    public void apply() {
        Config config = RulePersistentConfig.getInstance().getState();
        config.setUpdateUrl(this.remoteUrl.getText());
        String text = dataEditor.getDocument().getText();
        if (!this.saveData(text)) {
            return;
        }
        this.rawFileContent = text;
        ApplicationManager.getApplication().runWriteAction(() -> {
            String file = RuleDataService.class.getResource("/data/rule.js").getFile();
            try {
                FileUtils.write(new File(file), text,"utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private boolean saveData(String text) {
        JsonArray regArray = RuleDataService.getRegArray(text);
        if (regArray == null || regArray.size() == 0) {
            JBPopupFactory.getInstance().createBalloonBuilder(new JLabel("未匹配到正则"))
                    .setFillColor(contentPanel.getBackground())
                    .setAnimationCycle(0)
                    .setFadeoutTime(0)
                    .setRequestFocus(true)
                    .createBalloon()
                    .show(new RelativePoint(contentPanel, new Point(0, 0)), Balloon.Position.below);
            return false;
        }
        Config config = RulePersistentConfig.getInstance().getState();
        config.setRegExpArray(regArray);
        return true;
    }

    public void reset() {
        Config config = RulePersistentConfig.getInstance().getState();
        this.remoteUrl.setText(config.getUpdateUrl());
        ApplicationManager.getApplication().runWriteAction(()->{
            try {
                InputStream resourceAsStream = RuleDataService.class.getResourceAsStream("/data/rule.js");
                InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream, "utf-8");
                rawFileContent = FileUtil.loadTextAndClose(inputStreamReader);
                dataEditor.getDocument().setText(rawFileContent.replace("\r\n","\n"));
            } catch (Exception e) {

            }

        });
    }
}
