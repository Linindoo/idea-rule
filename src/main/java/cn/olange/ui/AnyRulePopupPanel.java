package cn.olange.ui;

import cn.olange.model.RuleModel;
import cn.olange.model.UsageTableCellRenderer;
import cn.olange.service.RuleDataService;
import cn.olange.setting.SettingConfigurable;
import cn.olange.utils.RuleUtil;
import com.intellij.find.SearchTextArea;
import com.intellij.find.actions.ShowUsagesAction;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.progress.util.ReadTask;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LoadingDecorator;
import com.intellij.openapi.ui.OnePixelDivider;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeGlassPane;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.*;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;
import com.intellij.util.Alarm;
import com.intellij.util.ui.*;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.JXTextArea;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AnyRulePopupPanel extends JBPanel<AnyRulePopupPanel> {
	private static final String IGNORE_SWAY_ROD = "any.rule.ignoreSwayRod";
	private String key = "ide.anyrule.enter.as.ok";
	private final Disposable myDisposable;
	private Project project;
	private Editor editor;
	private DialogWrapper myDialog;
	private JBLabel myTitleLabel;
	private JPanel myTitlePanel;
	private LoadingDecorator myLoadingDecorator;
	private SearchTextArea mySearchTextArea;
	private JXTextArea mySearchComponent;
	private JBTable myResultsPreviewTable;
	private final AtomicBoolean myCanClose;
	private final AtomicBoolean myIsPinned;
	private Alarm mySearchRescheduleOnCancellationsAlarm;
	private volatile ProgressIndicatorBase myResultsPreviewSearchProgress;
	private int myLoadingHash;
	private JButton myOKButton;
	private final Alarm myPreviewUpdater;
	private RulePreviewPanel rulePreviewPanel;
	private StateRestoringCheckBox ignoreSwayRod;
	public static final String SERVICE_KEY = "any.rule";
	public static final String SPLITTER_SERVICE_KEY = "any.rule.splitter";


	public AnyRulePopupPanel(Project project, Editor editor) {
		super();
		this.project = project;
		this.editor = editor;
		this.myDisposable = Disposer.newDisposable();
		this.myCanClose = new AtomicBoolean(true);
		this.myIsPinned = new AtomicBoolean(false);
		this.myPreviewUpdater = new Alarm(this.myDisposable);
		Disposer.register(this.myDisposable, () -> {
			this.finishPreviousPreviewSearch();
			if (this.mySearchRescheduleOnCancellationsAlarm != null) {
				Disposer.dispose(this.mySearchRescheduleOnCancellationsAlarm);
			}
		});
		initComponents();
	}

	private void initComponents() {
		this.setLayout(new MigLayout("flowx, ins 4, gap 0, fillx, hidemode 3"));
		this.myTitleLabel = new JBLabel("anyrule", UIUtil.ComponentStyle.REGULAR);
		this.myTitleLabel.setFont(this.myTitleLabel.getFont().deriveFont(1));
		this.myLoadingDecorator = new LoadingDecorator(new JLabel(EmptyIcon.ICON_16), this.getDisposable(), 250, true, new AsyncProcessIcon("FindInPathLoading"));
		this.myLoadingDecorator.setLoadingText("");
		this.myTitlePanel = new JPanel(new MigLayout("flowx, ins 0, gap 0, fillx, filly"));
		this.myTitlePanel.add(this.myTitleLabel);
		this.myTitlePanel.add(this.myLoadingDecorator.getComponent(), "w 24, wmin 24");
		JButton settingBtn = new JButton(AllIcons.General.Settings);
		settingBtn.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingConfigurable.DISPLAY_NAME);
			}
		});
		this.myTitlePanel.add(Box.createHorizontalGlue(), "growx, pushx");


		this.myOKButton = new JButton("双击插入");
		this.myOKButton.addActionListener((e) -> {
			AnyRulePopupPanel.this.insertRuleToDocument();
		});

		this.add(this.myTitlePanel, "sx 2, growx, growx, growy");
		this.add(settingBtn, "w 24, wmin 24, gapleft 4, gapright 4,wrap");
		this.mySearchComponent = new JXTextArea();
		this.mySearchComponent.setColumns(25);
		this.mySearchComponent.setRows(1);
		this.mySearchTextArea = new SearchTextArea(this.mySearchComponent, true, true);
		this.mySearchTextArea.setFocusable(true);
		this.mySearchTextArea.setMultilineEnabled(false);
		this.add(this.mySearchTextArea, "pushx, growx, sx 10, gaptop 4, wrap");
		this.mySearchRescheduleOnCancellationsAlarm = new Alarm();
		this.myResultsPreviewTable = new JBTable() {
			public Dimension getPreferredScrollableViewportSize() {
				return new Dimension(this.getWidth(), 1 + this.getRowHeight() * 4);
			}
		};
		this.myResultsPreviewTable.setFocusable(false);
		this.myResultsPreviewTable.getEmptyText().setShowAboveCenter(false);
		this.myResultsPreviewTable.setShowColumns(false);
		this.myResultsPreviewTable.setShowGrid(false);
		this.myResultsPreviewTable.setIntercellSpacing(JBUI.emptySize());
		this.myResultsPreviewTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		(new DoubleClickListener() {
			protected boolean onDoubleClick(MouseEvent event) {
				if (event.getSource() != AnyRulePopupPanel.this.myResultsPreviewTable) {
					return false;
				} else {
					AnyRulePopupPanel.this.insertRuleToDocument();
					return true;
				}
			}
		}).installOn(this.myResultsPreviewTable);
		this.myResultsPreviewTable.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				AnyRulePopupPanel.this.myResultsPreviewTable.transferFocus();
			}
		});
		ScrollingUtil.installActions(this.myResultsPreviewTable, false, mySearchComponent);

		JBScrollPane scrollPane = new JBScrollPane(this.myResultsPreviewTable) {
			public Dimension getMinimumSize() {
				Dimension size = super.getMinimumSize();
				size.height = AnyRulePopupPanel.this.myResultsPreviewTable.getPreferredScrollableViewportSize().height;
				return size;
			}
		};
		scrollPane.setBorder(JBUI.Borders.empty());
		JBSplitter splitter = new JBSplitter(true, 0.33F);
		splitter.setSplitterProportionKey(SPLITTER_SERVICE_KEY);
		splitter.setDividerWidth(1);
		splitter.getDivider().setBackground(OnePixelDivider.BACKGROUND);
		splitter.setFirstComponent(scrollPane);
		this.rulePreviewPanel = new RulePreviewPanel(this.project) {
			public Dimension getPreferredSize() {
				return new Dimension(AnyRulePopupPanel.this.myResultsPreviewTable.getWidth(), this.getHeight());
			}
		};
		Disposer.register(this.myDisposable, rulePreviewPanel);

		final Runnable updatePreviewRunnable = () -> {
			if (!Disposer.isDisposed(this.myDisposable)) {
				int selectedRow = this.myResultsPreviewTable.getSelectedRow();
				if (selectedRow >= 0) {
					Object data = this.myResultsPreviewTable.getModel().getValueAt(selectedRow, 0);
					rulePreviewPanel.updateLayout((RuleModel) data);
				}
			}
		};

		splitter.setSecondComponent(this.rulePreviewPanel);
		this.rulePreviewPanel.setVisible(false);
		this.myResultsPreviewTable.getSelectionModel().addListSelectionListener((e) -> {
			if (!e.getValueIsAdjusting() && !Disposer.isDisposed(this.myPreviewUpdater)) {
				this.myPreviewUpdater.addRequest(updatePreviewRunnable, 50);
			}
		});
		this.add(splitter, "pushx, growx, growy, pushy, sx 10, wrap, pad 4 4 4 4");
		DocumentAdapter documentAdapter = new DocumentAdapter() {
			protected void textChanged(@NotNull javax.swing.event.DocumentEvent e) {
				AnyRulePopupPanel.this.mySearchComponent.setRows(Math.max(1, Math.min(1, StringUtil.countChars(AnyRulePopupPanel.this.mySearchComponent.getText(), '\n') + 1)));
				if (AnyRulePopupPanel.this.myDialog != null) {
					if (e.getDocument() == AnyRulePopupPanel.this.mySearchComponent.getDocument()) {
						AnyRulePopupPanel.this.scheduleResultsUpdate();
					}
				}
			}
		};
		this.mySearchComponent.getDocument().addDocumentListener(documentAdapter);
		this.ignoreSwayRod = createCheckBox("自动去除首尾斜杆");
		PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(this.project);
		boolean ignore = propertiesComponent.getBoolean(IGNORE_SWAY_ROD, false);
		this.ignoreSwayRod.setSelected(ignore);
		this.ignoreSwayRod.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				propertiesComponent.setValue(IGNORE_SWAY_ROD, ignoreSwayRod.isSelected());
			}
		});
		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(ignoreSwayRod, BorderLayout.CENTER);
		bottomPanel.add(myOKButton, BorderLayout.EAST);
		this.add(bottomPanel, "pushx, growx, dock south, sx 10");
	}

	@NotNull
	private static StateRestoringCheckBox createCheckBox(String message) {
		StateRestoringCheckBox checkBox = new StateRestoringCheckBox(message);
		checkBox.setFocusable(false);
		return checkBox;
	}

	public void scheduleResultsUpdate() {
		if (this.myDialog != null && this.myDialog.isVisible()) {
			if (this.mySearchRescheduleOnCancellationsAlarm != null && !this.mySearchRescheduleOnCancellationsAlarm.isDisposed()) {
				this.mySearchRescheduleOnCancellationsAlarm.cancelAllRequests();
				this.mySearchRescheduleOnCancellationsAlarm.addRequest(this::findSettingsChanged, 100);
			}
		}
	}

	public void showUI() {
		if (this.myDialog == null || !this.myDialog.isVisible()) {
			if (this.myDialog != null && !Disposer.isDisposed(this.myDialog.getDisposable())) {
				this.myDialog.doCancelAction();
			}

			if (this.myDialog == null || Disposer.isDisposed(this.myDialog.getDisposable())) {
				myDialog = new DialogWrapper(this.project, true, DialogWrapper.IdeModalityType.MODELESS) {
					{
						init();
						getContentPane().add(new JLabel(), BorderLayout.SOUTH);//remove hardcoded southSection
						getRootPane().setDefaultButton(null);
					}


					@Override
					protected void dispose() {
						super.dispose();
					}

					@NotNull
					@Override
					protected Action[] createLeftSideActions() {
						return new Action[0];
					}

					@NotNull
					@Override
					protected Action[] createActions() {
						return new Action[0];
					}

					@Nullable
					@Override
					protected Border createContentPaneBorder() {
						return null;
					}

					@Override
					protected JComponent createCenterPanel() {
						return AnyRulePopupPanel.this;
					}

					@Override
					protected String getDimensionServiceKey() {
						return SERVICE_KEY;
					}
				};
				this.myDialog.setUndecorated(true);
				Disposer.register(project, AnyRulePopupPanel.this::closeImmediately);
				Disposer.register(this.myDialog.getDisposable(), this.myDisposable);
				Window window = WindowManager.getInstance().suggestParentWindow(this.project);
				Component parent = UIUtil.findUltimateParent(window);
				RelativePoint showPoint = null;
				Point screenPoint = DimensionService.getInstance().getLocation(SERVICE_KEY);
				if (screenPoint != null) {
					if (parent != null) {
						SwingUtilities.convertPointFromScreen(screenPoint, parent);
						showPoint = new RelativePoint(parent, screenPoint);
					} else {
						showPoint = new RelativePoint(screenPoint);
					}
				}

				if (parent != null && showPoint == null) {
					int height = UISettings.getInstance().getShowNavigationBar() ? 135 : 115;
					showPoint = new RelativePoint(parent, new Point((parent.getSize().width - this.getPreferredSize().width) / 2, height));
				}

				WindowMoveListener windowListener = new WindowMoveListener(this);
				this.addMouseListener(windowListener);
				this.addMouseMotionListener(windowListener);
				Dimension panelSize = this.getPreferredSize();
				Dimension prev = DimensionService.getInstance().getSize(SERVICE_KEY);

				panelSize.width += JBUI.scale(24);
				panelSize.height *= 2;
				if (prev != null && prev.height < panelSize.height) {
					prev.height = panelSize.height;
				}

				final Window w = this.myDialog.getPeer().getWindow();
				AnAction escape = ActionManager.getInstance().getAction("EditorEscape");
				JRootPane root = ((RootPaneContainer) w).getRootPane();
				final IdeGlassPane glass = (IdeGlassPane) this.myDialog.getRootPane().getGlassPane();
				int i = Registry.intValue("ide.popup.resizable.border.sensitivity", 4);
				WindowResizeListener resizeListener = new WindowResizeListener(root, JBUI.insets(i), (Icon) null) {
					private Cursor myCursor;

					protected void setCursor(Component content, Cursor cursor) {
						if (this.myCursor != cursor || this.myCursor != Cursor.getDefaultCursor()) {
							glass.setCursor(cursor, this);
							this.myCursor = cursor;
							if (content instanceof JComponent) {
								JComponent component = (JComponent) content;
								if (component.getClientProperty("SuperCursor") == null) {
									component.putClientProperty("SuperCursor", cursor);
								}
							}
							super.setCursor(content, cursor);
						}

					}
				};
				glass.addMousePreprocessor(resizeListener, this.myDisposable);
				glass.addMouseMotionPreprocessor(resizeListener, this.myDisposable);
				DumbAwareAction.create((e) -> {
					AnyRulePopupPanel.this.closeImmediately();
				}).registerCustomShortcutSet(escape == null ? CommonShortcuts.ESCAPE : escape.getShortcutSet(), root, this.myDisposable);
				root.setWindowDecorationStyle(0);
				if (SystemInfo.isMac && UIUtil.isUnderDarcula()) {
					root.setBorder(PopupBorder.Factory.createColored(OnePixelDivider.BACKGROUND));
				} else {
					root.setBorder(PopupBorder.Factory.create(true, true));
				}

				w.setBackground(UIUtil.getPanelBackground());
				w.setMinimumSize(panelSize);
				if (prev == null) {
					panelSize.height = (int) ((double) panelSize.height * 1.5D);
					panelSize.width = (int) ((double) panelSize.width * 1.15D);
				}

				w.setSize(prev != null ? prev : panelSize);
				IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
				if (showPoint != null) {
					this.myDialog.setLocation(showPoint.getScreenPoint());
				} else {
					w.setLocationRelativeTo(parent);
				}
				this.myDialog.show();
				this.mySearchComponent.requestFocus();
				w.addWindowListener(new WindowAdapter() {
					public void windowOpened(WindowEvent e) {
						w.addWindowFocusListener(new WindowAdapter() {
							public void windowLostFocus(WindowEvent e) {
								Window oppositeWindow = e.getOppositeWindow();
								if (oppositeWindow != w && (oppositeWindow == null || oppositeWindow.getOwner() != w)) {
									if (AnyRulePopupPanel.this.canBeClosed() || !AnyRulePopupPanel.this.myIsPinned.get() && oppositeWindow != null) {
										AnyRulePopupPanel.this.myDialog.doCancelAction();
									}

								}
							}
						});
					}
				});
			}
			ApplicationManager.getApplication().invokeLater(this::scheduleResultsUpdate, ModalityState.any());
		}
	}

	@NotNull
	public Disposable getDisposable() {
		return this.myDisposable;
	}

	private void closeImmediately() {
		if (this.canBeClosedImmediately() && this.myDialog != null && this.myDialog.isVisible()) {
			this.myIsPinned.set(false);
			this.myDialog.doCancelAction();
		}

	}

	public void insertRuleToDocument() {
		if (this.canBeClosed()) {
			this.myDialog.doCancelAction();
		}
		int selectedRow = this.myResultsPreviewTable.getSelectedRow();
		if (selectedRow < 0) {
			return;
		}
		if (this.editor == null) {
			return;
		}
		RuleModel value = (RuleModel) this.myResultsPreviewTable.getModel().getValueAt(selectedRow, 0);
		ApplicationManager.getApplication().runWriteAction(() -> {
			int offset = editor.getCaretModel().getOffset();
			WriteCommandAction.Builder builder = WriteCommandAction.writeCommandAction(AnyRulePopupPanel.this.project);
			builder.run(() -> {
				String  rule = value.getRule();
				editor.getDocument().insertString(offset, convertRule(rule));
			});
		});
	}

	private String convertRule(String rule) {
		if (rule == null) {
			return "";
		}
		if (this.ignoreSwayRod.isSelected()) {
			return RuleUtil.convertRule(rule);
		}
		return rule;


	}

	private boolean canBeClosedImmediately() {
		boolean state = this.myIsPinned.get();
		this.myIsPinned.set(false);

		boolean var2;
		try {
			var2 = this.myDialog != null && this.canBeClosed();
		} finally {
			this.myIsPinned.set(state);
		}

		return var2;
	}

	protected boolean canBeClosed() {
		if (this.project.isDisposed()) {
			return true;
		} else if (!this.myCanClose.get()) {
			return false;
		} else if (this.myIsPinned.get()) {
			return false;
		} else if (!ApplicationManager.getApplication().isActive()) {
			return false;
		} else if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow() == null) {
			return false;
		} else {
			return true;
		}
	}

	private void showEmptyText(@Nullable String message) {
		StatusText emptyText = this.myResultsPreviewTable.getEmptyText();
		emptyText.clear();
		emptyText.setText(message != null ? UIBundle.message("message.nothingToShow.with.problem", new Object[]{message}) : UIBundle.message("message.nothingToShow", new Object[0]));
	}

	private void finishPreviousPreviewSearch() {
		if (this.myResultsPreviewSearchProgress != null && !this.myResultsPreviewSearchProgress.isCanceled()) {
			this.myResultsPreviewSearchProgress.cancel();
		}

	}

	private void findSettingsChanged() {
		if (this.isShowing()) {
			ScrollingUtil.ensureSelectionExists(this.myResultsPreviewTable);
		}
		final ModalityState state = ModalityState.current();
		this.finishPreviousPreviewSearch();
		this.mySearchRescheduleOnCancellationsAlarm.cancelAllRequests();
		final ProgressIndicatorBase progressIndicatorWhenSearchStarted = new ProgressIndicatorBase() {
			public void stop() {
				super.stop();
				AnyRulePopupPanel.this.onStop(System.identityHashCode(this));
			}
		};
		this.myResultsPreviewSearchProgress = progressIndicatorWhenSearchStarted;
		final int hash = System.identityHashCode(this.myResultsPreviewSearchProgress);
		final DefaultTableModel model = new DefaultTableModel() {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		model.addColumn("Usages");
		this.mySearchTextArea.setInfoText(null);
		this.myResultsPreviewTable.setModel(model);
		this.myResultsPreviewTable.getColumnModel().getColumn(0).setCellRenderer(new UsageTableCellRenderer());
		this.onStart(hash);
		final AtomicInteger resultsCount = new AtomicInteger();
		ProgressIndicatorUtils.scheduleWithWriteActionPriority(this.myResultsPreviewSearchProgress, new ReadTask() {
			public ReadTask.Continuation performInReadAction(@NotNull ProgressIndicator indicator) {
				if (this.isCancelled()) {
					AnyRulePopupPanel.this.onStop(hash);
				} else {
					ApplicationManager.getApplication().invokeLater(() -> {
						if (this.isCancelled()) {
							AnyRulePopupPanel.this.onStop(hash);
						} else {
							RuleDataService.filterRule(AnyRulePopupPanel.this.mySearchComponent.getText(), (result) -> {
								if (result.isSuccess()) {
									List<RuleModel> array = result.getResult();
									for (int i = 0; i < array.size(); i++) {
										RuleModel rule = array.get(i);
										model.insertRow(i, new Object[]{rule});
									}
									int occurrences = resultsCount.get();
									if (model.getRowCount() > 0 && AnyRulePopupPanel.this.myResultsPreviewTable.getModel() == model) {
										AnyRulePopupPanel.this.rulePreviewPanel.setVisible(occurrences > 0);
										AnyRulePopupPanel.this.myResultsPreviewTable.setRowSelectionInterval(0, 0);
									} else {
										AnyRulePopupPanel.this.rulePreviewPanel.setVisible(false);
									}
								}
							});
						}
					}, state);
					boolean continueSearch = resultsCount.incrementAndGet() < ShowUsagesAction.getUsagesPageSize();
					if (!continueSearch) {
						AnyRulePopupPanel.this.onStop(hash);
					}
				}
				return new Continuation(() -> {
					if (!this.isCancelled() && resultsCount.get() == 0) {
						AnyRulePopupPanel.this.showEmptyText(null);
					}

					AnyRulePopupPanel.this.onStop(hash);
				}, state);
			}

			boolean isCancelled() {
				return progressIndicatorWhenSearchStarted != AnyRulePopupPanel.this.myResultsPreviewSearchProgress || progressIndicatorWhenSearchStarted.isCanceled();
			}

			public void onCanceled(@NotNull ProgressIndicator indicator) {
				if (AnyRulePopupPanel.this.isShowing() && progressIndicatorWhenSearchStarted == AnyRulePopupPanel.this.myResultsPreviewSearchProgress) {
					AnyRulePopupPanel.this.scheduleResultsUpdate();
				}
			}
		});
	}

	private void onStart(int hash) {
		this.myLoadingHash = hash;
		this.myLoadingDecorator.startLoading(false);
		this.myResultsPreviewTable.getEmptyText().setText("Searching...");
	}

	private void onStop(int hash) {
		this.onStop(hash, null);
	}

	private void onStop(int hash, String message) {
		if (hash == this.myLoadingHash) {
			UIUtil.invokeLaterIfNeeded(() -> {
				this.showEmptyText(message);
				this.myLoadingDecorator.stopLoading();
			});
		}
	}
}
