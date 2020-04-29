package cn.olange.ui;

import com.google.gson.JsonObject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import groovy.json.StringEscapeUtils;
import org.jdesktop.swingx.JXTextArea;

import java.awt.*;

public class RulePreviewPanel extends JBPanelWithEmptyText implements Disposable {
	private JXTextArea textArea;
	private JXTextArea examples;
	private Project project;

	public RulePreviewPanel(Project project) {
		super();
		this.project = project;
		this.setLayout(new BorderLayout());
		this.setBorder(JBUI.Borders.empty());
		textArea = new JXTextArea();
		JBScrollPane scrollPane = new JBScrollPane(this.textArea);
		scrollPane.setBorder(JBUI.Borders.empty());
		this.add(scrollPane,BorderLayout.NORTH);
		this.examples = new JXTextArea();
		JBScrollPane exampleScrollPanel = new JBScrollPane(this.examples);
		exampleScrollPanel.setBorder(IdeBorderFactory.createTitledBorder("示例", false, new JBInsets(8, 0, 0, 0)).setShowLine(false));
		this.add(exampleScrollPanel,BorderLayout.CENTER);

	}

	public void updateLayout(JsonObject row) {
		ApplicationManager.getApplication().invokeLater(()->{RulePreviewPanel.this.updateLayoutLater(row);}, ModalityState.any());
	}

	public void updateLayoutLater(JsonObject row) {
		String title = row.get("title").getAsString();
		String rule = row.get("rule").getAsString();
		String examples = row.get("examples").getAsString();
		this.setBorder(IdeBorderFactory.createTitledBorder(title, false, new JBInsets(8, 0, 0, 0)).setShowLine(false));
		this.setToolTipText(title);
		this.textArea.setText(StringEscapeUtils.escapeJava(rule));
		this.examples.setText(examples);
	}

	@Override
	public void dispose() {
	}
}
