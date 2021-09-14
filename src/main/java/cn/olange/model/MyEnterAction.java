package cn.olange.model;

import cn.olange.ui.AnyRulePopupPanel;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

public class MyEnterAction extends DumbAwareAction {

	private final boolean myEnterAsOK;
	private AnyRulePopupPanel anyRulePopupPanel;

	public MyEnterAction(boolean enterAsOK, AnyRulePopupPanel anyRulePopupPanel) {
		this.myEnterAsOK = enterAsOK;
		this.anyRulePopupPanel = anyRulePopupPanel;
	}

	public void update(@NotNull AnActionEvent e) {
		e.getPresentation().setEnabled(e.getData(CommonDataKeys.EDITOR) == null);
	}

	public void actionPerformed(@NotNull AnActionEvent e) {
		anyRulePopupPanel.insertRuleToDocument();
	}
}
