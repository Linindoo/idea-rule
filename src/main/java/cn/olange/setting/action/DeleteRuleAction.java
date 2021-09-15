package cn.olange.setting.action;

import cn.olange.setting.SettingUI;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class DeleteRuleAction extends DumbAwareAction {


	private SettingUI settingUI;

	public DeleteRuleAction(SettingUI settingUI) {
		super("删除规则", "", AllIcons.Actions.Cancel);
		this.settingUI = settingUI;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
		settingUI.deleteSelectRow();
	}
}
