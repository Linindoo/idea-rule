package cn.olange.model;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class UsageTableCellRenderer extends JPanel implements TableCellRenderer {
	private static final int MARGIN = 2;
	private JBLabel title;
	private final ColoredTableCellRenderer myRule;


	public UsageTableCellRenderer() {
		setLayout(new BorderLayout());
		title = new JBLabel();
		myRule = new RuleTableCellRender();
		add(myRule, BorderLayout.CENTER);
		add(title, BorderLayout.WEST);
		setBorder(JBUI.Borders.empty(MARGIN, MARGIN, MARGIN, 0));
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		myRule.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		Color color = isSelected ? table.getSelectionBackground() : table.getBackground();
		setBackground(color);
		title.setBackground(color);
		myRule.setBackground(color);
		if (value instanceof RuleModel) {
			RuleModel ruleObj = (RuleModel) value;
			this.title.setText(ruleObj.getTitle());
			if (ruleObj.isSelfBuild()) {
				this.title.setIcon(IconLoader.findIcon("/icons/selfbuild.svg"));
			} else {
				this.title.setIcon(null);
			}
		}
		return this;
	}

}
