package cn.olange.model;

import com.google.gson.JsonObject;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.*;

public class RuleTableCellRender extends ColoredTableCellRenderer {

	@Override
	protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
		if (value instanceof RuleModel) {
			RuleModel ruleObj = (RuleModel) value;
			this.append("  " + ruleObj.getRule(), SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
		}
		setBorder(null);
	}
}
