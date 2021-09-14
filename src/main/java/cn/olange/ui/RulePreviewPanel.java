package cn.olange.ui;

import cn.olange.model.RuleModel;
import cn.olange.utils.RuleUtil;
import com.google.gson.JsonObject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBPanelWithEmptyText;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXTextArea;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.TextUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RulePreviewPanel extends JBPanelWithEmptyText implements Disposable {
	private JXTextArea textArea;
	private Project project;
	private JLabel statusText;
	private JTextArea checkTextArea;
	private Alarm checkAlarm;

	public static final KeyStroke NEW_LINE_KEYSTROKE = KeyStroke.getKeyStroke(10, (SystemInfo.isMac ? 256 : 128) | 64);



	public RulePreviewPanel(Project project) {
		super();
		this.project = project;
		this.checkAlarm = new Alarm();
		this.setLayout(new BorderLayout());
		this.setBorder(JBUI.Borders.empty());
		textArea = new JXTextArea();
		this.textArea.setEditable(false);
		this.checkTextArea = new JTextArea();
		this.checkTextArea.registerKeyboardAction((e) -> {
			if (this.checkTextArea.getText().contains("\n")) {
				if (this.checkTextArea.isEditable() && this.checkTextArea.isEnabled()) {
					this.checkTextArea.replaceSelection("\t");
				} else {
					UIManager.getLookAndFeel().provideErrorFeedback(this.checkTextArea);
				}
			} else {
				this.checkTextArea.transferFocus();
			}

		}, KeyStroke.getKeyStroke(9, 0), 0);
		this.checkTextArea.registerKeyboardAction((e) -> {
			this.checkTextArea.transferFocusBackward();
		}, KeyStroke.getKeyStroke(9, 64), 0);
		KeymapUtil.reassignAction(this.checkTextArea, KeyStroke.getKeyStroke(10, 0), NEW_LINE_KEYSTROKE, 0);
		this.checkTextArea.setDocument(new PlainDocument() {
			public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
				if (this.getProperty("filterNewlines") == Boolean.TRUE && str.indexOf(10) >= 0) {
					str = StringUtil.replace(str, "\n", " ");
				}

				if (!StringUtil.isEmpty(str)) {
					super.insertString(offs, str, a);
				}

			}
		});
		this.checkTextArea.getDocument().putProperty("trimTextOnPaste", Boolean.TRUE);
		this.checkTextArea.getDocument().addDocumentListener(new DocumentAdapter() {
			protected void textChanged(DocumentEvent e) {
				RulePreviewPanel.this.checkTextChange();
			}
		});
		this.checkTextArea.setOpaque(false);

		JBScrollPane jbScrollPane = new JBScrollPane(this.checkTextArea, 20, 30) {
			public Dimension getPreferredSize() {
				Dimension d = super.getPreferredSize();
				TextUI ui = RulePreviewPanel.this.checkTextArea.getUI();
				if (ui != null) {
					d.height = Math.min(d.height, ui.getPreferredSize(RulePreviewPanel.this.checkTextArea).height);
				}

				return d;
			}
		};
		this.checkTextArea.setBorder(new Border() {
			public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			}

			public Insets getBorderInsets(Component c) {
				if (SystemInfo.isMac && !UIUtil.isUnderDarcula()) {
					return new JBInsets(3, 0, 3, 0);
				} else {
					int bottom = StringUtil.getLineBreakCount(RulePreviewPanel.this.checkTextArea.getText()) > 0 ? 2 : (UIUtil.isUnderDarcula() ? 2 : 1);
					int top = RulePreviewPanel.this.checkTextArea.getFontMetrics(RulePreviewPanel.this.checkTextArea.getFont()).getHeight() <= 16 ? 2 : 1;
					if (JBUI.isUsrHiDPI()) {
						bottom = 2;
						top = 2;
					}

					return new JBInsets(top, 0, bottom, 0);
				}
			}

			public boolean isBorderOpaque() {
				return false;
			}
		});
		JBScrollPane scrollPane = new JBScrollPane(this.textArea);
		scrollPane.setBorder(JBUI.Borders.empty());
		this.add(scrollPane,BorderLayout.NORTH);
		jbScrollPane.setBorder(IdeBorderFactory.createTitledBorder("验证", false, new JBInsets(8, 0, 0, 0)).setShowLine(true));
		this.add(jbScrollPane, BorderLayout.CENTER);
		JPanel bootomPanel = new JPanel();
		bootomPanel.setLayout(new BorderLayout());
		statusText = new JLabel();
		bootomPanel.add(statusText, BorderLayout.WEST);
		this.add(bootomPanel, BorderLayout.SOUTH);

	}

	private void checkTextChange(){
		if (this.checkAlarm != null && !this.checkAlarm.isDisposed()) {
			this.checkAlarm.cancelAllRequests();
			this.checkAlarm.addRequest(new Runnable() {
				@Override
				public void run() {
					String text = checkTextArea.getText();
					if (StringUtils.isNotEmpty(text)) {
						String rule = RuleUtil.convertRule(textArea.getText());
						Pattern pattern = Pattern.compile(rule);
						Matcher matcher = pattern.matcher(text);
						if (matcher.find()) {
							statusText.setForeground(Color.GREEN);
							statusText.setText("验证成功，符合规则");
						} else {
							statusText.setForeground(Color.RED);
							statusText.setText("验证失败");
						}
					} else {
						statusText.setText("");
					}
				}
			}, 200);
		}
	}

	public void updateLayout(RuleModel ruleModel) {
		ApplicationManager.getApplication().invokeLater(()->{RulePreviewPanel.this.updateLayoutLater(ruleModel);}, ModalityState.any());
	}

	public void updateLayoutLater(RuleModel row) {
		String title = row.getTitle();
		String rule = row.getRule();
		String examples = row.getExamples();
		this.setBorder(IdeBorderFactory.createTitledBorder(title, false, new JBInsets(8, 0, 0, 0)).setShowLine(false));
		this.setToolTipText(title);
		this.textArea.setText(rule);
		this.checkTextArea.setToolTipText(examples);
		this.checkTextArea.setText("");
	}

	@Override
	public void dispose() {
		if (this.checkAlarm != null && !this.checkAlarm.isDisposed()) {
			this.checkAlarm.cancelAllRequests();
			this.checkAlarm.dispose();
		}
	}
}
