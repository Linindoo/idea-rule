package cn.olange;

import cn.olange.ui.AnyRulePopupPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;

public class AnyRuleFindAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		Editor editor = e.getData(CommonDataKeys.EDITOR);
		AnyRulePopupPanel popupPanel = new AnyRulePopupPanel(e.getProject(), editor);
		popupPanel.showUI();
	}
}
