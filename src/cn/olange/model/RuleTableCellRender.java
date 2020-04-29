package cn.olange.model;

import com.google.gson.JsonObject;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import groovy.json.StringEscapeUtils;

import javax.swing.*;

public class RuleTableCellRender extends ColoredTableCellRenderer {

	@Override
	protected void customizeCellRenderer(JTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
		if (value instanceof JsonObject) {
			JsonObject ruleObj = (JsonObject) value;
			this.append("  " + StringEscapeUtils.escapeJava(ruleObj.get("rule").getAsString()), SimpleTextAttributes.SYNTHETIC_ATTRIBUTES);
		}

		setBorder(null);
	}
}
