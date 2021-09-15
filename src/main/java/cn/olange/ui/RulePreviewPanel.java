package cn.olange.ui;

import cn.olange.model.RuleModel;
import cn.olange.utils.RuleUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.OnePixelDivider;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBSplitter;
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
	private JTextArea exampleText;

	public static final KeyStroke NEW_LINE_KEYSTROKE = KeyStroke.getKeyStroke(10, (SystemInfo.isMac ? 256 : 128) | 64);
	public static final String SPLITTER_SERVICE_KEY = "any.rule.preview.splitter";



	public RulePreviewPanel(Project project) {
		super();
		this.project = project;
		this.checkAlarm = new Alarm();
		this.setLayout(new BorderLayout());
		this.setBorder(JBUI.Borders.empty());
		textArea = new JXTextArea();
		this.textArea.setEditable(false);
		this.exampleText = new JXTextArea();
		this.exampleText.setEditable(false);
		this.exampleText.setLineWrap(true);
		this.exampleText.setWrapStyleWord(true);
		this.checkTextArea = new JTextArea();
		this.checkTextArea.setLineWrap(true);
		this.checkTextArea.setWrapStyleWord(true);
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
		JPanel bootomPanel = new JPanel();
		bootomPanel.setLayout(new BorderLayout());
		statusText = new JLabel();
		bootomPanel.add(statusText, BorderLayout.WEST);
		this.add(bootomPanel, BorderLayout.SOUTH);

		JBSplitter splitter = new JBSplitter(false,0.55F);
		splitter.setSplitterProportionKey(SPLITTER_SERVICE_KEY);
		splitter.setDividerWidth(1);
		splitter.getDivider().setBackground(OnePixelDivider.BACKGROUND);
		splitter.setFirstComponent(jbScrollPane);
		JBScrollPane exampleScroll = new JBScrollPane(this.exampleText, 20, 30);
		exampleScroll.setBorder(JBUI.Borders.empty());
		exampleScroll.setBorder(IdeBorderFactory.createTitledBorder("示例", true, new JBInsets(8, 10, 0, 0)).setShowLine(true));
		splitter.setSecondComponent(exampleScroll);
		this.add(splitter, BorderLayout.CENTER);


	}

	private void checkTextChange(){
		if (this.checkAlarm != null && !this.checkAlarm.isDisposed()) {
			this.checkAlarm.cancelAllRequests();
			this.checkAlarm.addRequest(() -> {
				String text = checkTextArea.getText();
				if (StringUtils.isNotEmpty(text)) {
					String rule = RuleUtil.convertRule(textArea.getText());
					Pattern pattern = Pattern.compile(rule);
					Matcher matcher = pattern.matcher(text);
					if (matcher.find()) {
						statusText.setIcon(AllIcons.RunConfigurations.TestPassed);
					} else {
						statusText.setIcon(AllIcons.RunConfigurations.TestError);
					}
				} else {
					statusText.setIcon(null);
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
		this.exampleText.setText(examples);
	}

	@Override
	public void dispose() {
		if (this.checkAlarm != null && !this.checkAlarm.isDisposed()) {
			this.checkAlarm.cancelAllRequests();
			this.checkAlarm.dispose();
		}
	}
}
