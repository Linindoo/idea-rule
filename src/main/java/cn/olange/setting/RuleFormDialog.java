package cn.olange.setting;

import cn.olange.model.RuleModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RuleFormDialog extends DialogWrapper {
    private JPanel main;
    private JTextField nameField;
    private JTextField ruleField;
    private JTextArea exampleField;
    private SettingUI settingUI;
    private RuleModel ruleModel;

    public RuleFormDialog(@Nullable Project project, SettingUI settingUI, RuleModel ruleModel) {
        super(project, true);
        this.settingUI = settingUI;
        if (ruleModel != null) {
            this.nameField.setText(ruleModel.getTitle());
            this.ruleField.setText(ruleModel.getRule());
            this.exampleField.setText(ruleModel.getExamples());
            this.setTitle("修改规则");
            this.ruleModel = ruleModel;
        } else {
            this.setTitle("新增规则");
        }
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return main;
    }


    protected void doOKAction() {
        String rule = ruleField.getText();
        String title = nameField.getText();
        if (StringUtil.isEmpty(rule)) {
            this.setErrorText("规则不能为空");
            return;
        }
        if (StringUtil.isEmpty(title)) {
            this.setErrorText("规则名称不能为空");
            return;
        }
        if (ruleModel != null) {
            ruleModel.setRule(rule);
            ruleModel.setTitle(title);
            ruleModel.setExamples(exampleField.getText());
        } else {
            ruleModel = new RuleModel();
            ruleModel.setRule(rule);
            ruleModel.setTitle(title);
            ruleModel.setExamples(exampleField.getText());
            ruleModel.setSelfBuild(true);
            settingUI.addRule(ruleModel);
        }
        settingUI.updateTableUI();
        super.doOKAction();
    }

}
