package cn.olange.setting.action;

import cn.olange.setting.RuleFormDialog;
import cn.olange.setting.SettingUI;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class NewRuleAction extends DumbAwareAction {


	private SettingUI settingUI;

	public NewRuleAction(SettingUI settingUI) {
		super("添加规则", "", AllIcons.General.Add);
		this.settingUI = settingUI;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
		RuleFormDialog ruleFormDialog = new RuleFormDialog(anActionEvent.getProject(), settingUI, null);
		ruleFormDialog.show();
	}
}
