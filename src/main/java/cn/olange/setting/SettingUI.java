package cn.olange.setting;

import cn.olange.model.Config;
import cn.olange.model.RegExTableModel;
import cn.olange.model.RuleModel;
import cn.olange.service.RuleDataService;
import cn.olange.setting.action.DeleteRuleAction;
import cn.olange.setting.action.NewRuleAction;
import cn.olange.utils.HttpUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SettingUI {
    private JTextField remoteUrl;
    private JButton dataUpdate;
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JLabel helpbtn;
    private JTable reTable;
    private JPanel toolbar;
    private List<RuleModel> tmpRuleModels;


    public SettingUI() {
        JComponent actionToolbar = ActionManager.getInstance().createActionToolbar("操作", createActionGroup(), true).getComponent();
        this.toolbar.setLayout(new BorderLayout());
        this.toolbar.add(actionToolbar, BorderLayout.WEST);
        this.helpbtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                BrowserUtil.browse("https://github.com/Linindoo/idea-rule");
            }
        });
        reTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        (new DoubleClickListener() {
            protected boolean onDoubleClick(MouseEvent event) {
                if (event.getSource() != SettingUI.this.reTable) {
                    return false;
                } else {
                    RuleModel selectRow = SettingUI.this.getSelectRow();
                    if (selectRow != null) {
                        RuleFormDialog ruleFormDialog = new RuleFormDialog(null, SettingUI.this, selectRow);
                        ruleFormDialog.show();
                    }
                    return true;
                }
            }
        }).installOn(this.reTable);
        this.reTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                SettingUI.this.reTable.transferFocus();
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
                                saveRemoteData(ret);
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
                            dataUpdate.setEnabled(true);
                            JBPopupFactory.getInstance().createBalloonBuilder(new JLabel("获取失败，请检查地址是否正确或者网络是否可用"))
                                    .setFillColor(contentPanel.getBackground())
                                    .setAnimationCycle(0)
                                    .setFadeoutTime(0)
                                    .setRequestFocus(true)
                                    .createBalloon()
                                    .show(new RelativePoint(remoteUrl, new Point(10, 10)), Balloon.Position.below);
                        });
                    }
                });
            }
        });
    }

    private boolean ruleIsChange(List<RuleModel> oldRules) {
        if (oldRules == null && this.tmpRuleModels != null) {
            return true;
        }
        if (oldRules != null && this.tmpRuleModels == null) {
            return true;
        }
        if (oldRules == null) {
            return false;
        }
        if (oldRules.size() != tmpRuleModels.size()) {
            return true;
        }
        for (int i = 0; i < oldRules.size(); i++) {
            RuleModel oldRule = oldRules.get(i);
            if (!oldRule.equals(tmpRuleModels.get(i))) {
                return true;
            }
        }
        return false;
    }

    public RuleModel getSelectRow() {
        int selectedRow = this.reTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow > this.tmpRuleModels.size()) {
            return null;
        }
        return this.tmpRuleModels.get(selectedRow);
    }

    private ActionGroup createActionGroup() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new NewRuleAction(this));
        actionGroup.add(new DeleteRuleAction(this));
        return actionGroup;
    }

    public JComponent getComponent() {
        return mainPanel;
    }

    public boolean isModified() {
        Config config = RulePersistentConfig.getInstance().getState();
        return !Comparing.strEqual(config.getUpdateUrl(), remoteUrl.getText()) || ruleIsChange(config.getRegExpList());
    }

    public void apply() {
        Config config = RulePersistentConfig.getInstance().getState();
        config.setUpdateUrl(this.remoteUrl.getText());
        List<RuleModel> regExpList = new ArrayList<>();
        for (RuleModel ruleModel : tmpRuleModels) {
            RuleModel tmpRule = new RuleModel();
            tmpRule.setRule(ruleModel.getRule());
            tmpRule.setTitle(ruleModel.getTitle());
            tmpRule.setExamples(ruleModel.getExamples());
            tmpRule.setSelfBuild(ruleModel.isSelfBuild());
            regExpList.add(tmpRule);
        }
        config.setRegExpList(regExpList);
    }

    private void saveRemoteData(String text) {
        List<RuleModel> ruleModels = RuleDataService.getRegArray(text);
        if (ruleModels.size() == 0) {
            JBPopupFactory.getInstance().createBalloonBuilder(new JLabel("未匹配到正则"))
                    .setFillColor(contentPanel.getBackground())
                    .setAnimationCycle(0)
                    .setFadeoutTime(0)
                    .setRequestFocus(true)
                    .createBalloon()
                    .show(new RelativePoint(contentPanel, new Point(0, 0)), Balloon.Position.below);
            return;
        }
        Map<String, RuleModel> ruleMap = ruleModels.stream().collect(Collectors.toMap(RuleModel::getTitle, x -> x));
        for (RuleModel tmpRuleModel : this.tmpRuleModels) {
            if (!tmpRuleModel.isSelfBuild()) {
                RuleModel ruleModel = ruleMap.get(tmpRuleModel.getTitle());
                if (ruleModel != null) {
                    tmpRuleModel.setRule(ruleModel.getRule());
                    tmpRuleModel.setExamples(ruleModel.getExamples());
                }
            }
        }
        Map<String, RuleModel> tmpMapx = tmpRuleModels.stream().filter(x -> !x.isSelfBuild()).collect(Collectors.toMap(RuleModel::getTitle, x -> x));
        for (RuleModel ruleModel : ruleModels) {
            RuleModel newModel = tmpMapx.get(ruleModel.getTitle());
            if (newModel == null) {
                tmpRuleModels.add(ruleModel);
            }
        }
    }

    public void reset() {
        Config config = RulePersistentConfig.getInstance().getState();
        this.remoteUrl.setText(config.getUpdateUrl());
        List<RuleModel> regExpList = config.getRegExpList();
        this.tmpRuleModels = new ArrayList<>();
        for (RuleModel ruleModel : regExpList) {
            RuleModel tmpRule = new RuleModel();
            tmpRule.setRule(ruleModel.getRule());
            tmpRule.setTitle(ruleModel.getTitle());
            tmpRule.setExamples(ruleModel.getExamples());
            tmpRule.setSelfBuild(ruleModel.isSelfBuild());
            tmpRuleModels.add(tmpRule);
        }
        reTable.setModel(new RegExTableModel(this.tmpRuleModels));
    }

    public void addRule(RuleModel ruleModel) {
        tmpRuleModels.add(ruleModel);
    }

    public void updateTableUI() {
        this.reTable.updateUI();
    }

    public void deleteSelectRow() {
        int selectedRow = this.reTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow > this.tmpRuleModels.size()) {
            return;
        }
        this.tmpRuleModels.remove(selectedRow);
        this.updateTableUI();
    }
}
