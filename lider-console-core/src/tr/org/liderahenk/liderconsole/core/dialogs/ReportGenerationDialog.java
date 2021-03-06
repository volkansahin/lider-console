package tr.org.liderahenk.liderconsole.core.dialogs;

import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.liderconsole.core.constants.LiderConstants;
import tr.org.liderahenk.liderconsole.core.i18n.Messages;
import tr.org.liderahenk.liderconsole.core.model.ReportExportType;
import tr.org.liderahenk.liderconsole.core.model.ReportView;
import tr.org.liderahenk.liderconsole.core.model.ReportViewColumn;
import tr.org.liderahenk.liderconsole.core.model.ReportViewParameter;
import tr.org.liderahenk.liderconsole.core.rest.requests.ReportGenerationRequest;
import tr.org.liderahenk.liderconsole.core.rest.utils.ReportRestUtils;
import tr.org.liderahenk.liderconsole.core.utils.SWTResourceManager;
import tr.org.liderahenk.liderconsole.core.widgets.Notifier;

public class ReportGenerationDialog extends DefaultLiderDialog {

	private static final Logger logger = LoggerFactory.getLogger(ReportGenerationDialog.class);

	// Model
	private ReportView selectedView;
	// Widgets
	private Combo cmbExportType;
	private Composite paramContainer;
	private Composite tableContainer;
	private Button btnGenerateReport;
	private Label lblResult;

	private static final int DEFAULT_COLUMN_WIDTH = 100;

	public ReportGenerationDialog(Shell parentShell, ReportView selectedView) {
		super(parentShell);
		this.selectedView = selectedView;
	}

	/**
	 * Create template input widgets
	 */
	@Override
	protected Control createDialogArea(Composite parent) {

		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		parent.setLayout(new GridLayout(1, false));

		Composite innerComposite = new Composite(parent, SWT.NONE);
		innerComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		innerComposite.setLayout(new GridLayout(2, false));

		Label lblExportType = new Label(innerComposite, SWT.NONE);
		lblExportType.setText(Messages.getString("EXPORT_TYPE"));

		cmbExportType = new Combo(innerComposite, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbExportType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		ReportExportType[] types = ReportExportType.values();
		for (int i = 0; i < types.length; i++) {
			ReportExportType type = types[i];
			cmbExportType.add(type.getMessage());
			cmbExportType.setData(i + "", type);
		}
		cmbExportType.select(0);

		Set<ReportViewParameter> params = selectedView.getViewParams();
		if (params != null && !params.isEmpty()) {

			// Report parameters label
			Label lblParam = new Label(parent, SWT.NONE);
			lblParam.setFont(SWTResourceManager.getFont("Sans", 9, SWT.BOLD));
			lblParam.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			lblParam.setText(Messages.getString("REPORT_PARAMETERS"));

			paramContainer = new Composite(parent, SWT.BORDER);
			paramContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			paramContainer.setLayout(new GridLayout(2, false));
			for (ReportViewParameter param : params) {
				// Param label
				Label lbl = new Label(paramContainer, SWT.NONE);
				lbl.setText(param.getLabel());

				// Param input
				Text txt = new Text(paramContainer, SWT.BORDER);
				txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				// Associate parameter key with this input
				txt.setData(param.getReferencedParam().getKey());
				if (param.getValue() != null) {
					txt.setText(param.getValue());
				}
			}
		}

		// Report results label
		lblResult = new Label(parent, SWT.NONE);
		lblResult.setFont(SWTResourceManager.getFont("Sans", 9, SWT.BOLD));
		lblResult.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		lblResult.setText(Messages.getString("REPORT_RESULT"));
		lblResult.setVisible(false);

		tableContainer = new Composite(parent, SWT.NONE);
		tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableContainer.setLayout(new GridLayout(2, false));

		applyDialogFont(parent);
		return parent;
	}

	protected void generateTable(List<Object[]> list) {
		// Dispose previous table!
		disposePrev(tableContainer);
		if (list != null && !list.isEmpty()) {
			lblResult.setVisible(true);
			TableViewer tableViewer = SWTResourceManager.createTableViewer(tableContainer);
			createTableColumns(tableViewer, list);
			// Populate table
			tableViewer.setInput(list);
			tableViewer.refresh();
			// Redraw table
			tableContainer.layout(true, true);
		} else {
			lblResult.setVisible(false);
			disposePrev(tableContainer);
			Notifier.warning(null, Messages.getString("EMPTY_REPORT"));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static Object[] convertToObjectArray(Object array) {
		Class ofArray = array.getClass().getComponentType();
		if (ofArray.isPrimitive()) {
			List ar = new ArrayList();
			int length = Array.getLength(array);
			for (int i = 0; i < length; i++) {
				ar.add(Array.get(array, i));
			}
			return ar.toArray();
		} else {
			return (Object[]) array;
		}
	}

	/**
	 * 
	 * 
	 * @param tableViewer
	 * @param list
	 *            a collection of report fields
	 */
	private void createTableColumns(TableViewer tableViewer, List<Object[]> list) {

		Set<ReportViewColumn> columns = selectedView.getViewColumns();
		if (columns != null && !columns.isEmpty()) {
			for (final ReportViewColumn c : columns) {
				TableViewerColumn column = SWTResourceManager.createTableViewerColumn(tableViewer,
						c.getReferencedCol().getName(), c.getWidth() != null ? c.getWidth() : DEFAULT_COLUMN_WIDTH);
				column.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof Object[]) {
							Object[] fields = convertToObjectArray(element);
							int index = c.getReferencedCol().getColumnOrder() - 1;
							return (index >= fields.length || fields[index] == null) ? Messages.getString("UNTITLED")
									: fields[index].toString();
						}
						return Messages.getString("UNTITLED");
					}
				});
			}
		} else {
			// No column defined in the view, we should display all the
			// fields!
			for (int i = 0; i < list.get(0).length; i++) {
				TableViewerColumn column = SWTResourceManager.createTableViewerColumn(tableViewer, "",
						DEFAULT_COLUMN_WIDTH);
				final int j = i;
				column.setLabelProvider(new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof Object[]) {
							Object[] curRow = (Object[]) element;
							return curRow[j] != null ? curRow[j].toString() : "";
						}
						return Messages.getString("UNTITLED");
					}
				});
			}
		}
	}

	/**
	 * Dispose previous table if exists
	 * 
	 * @param composite
	 */
	private void disposePrev(Composite composite) {
		Control[] children = composite.getChildren();
		if (children != null) {
			for (Control child : children) {
				child.dispose();
			}
		}
	}

	protected boolean validateInputs() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// Button for generating report table
		btnGenerateReport = createButton(parent, 5000, Messages.getString("GENERATE_REPORT"), false);
		btnGenerateReport.setImage(
				SWTResourceManager.getImage(LiderConstants.PLUGIN_IDS.LIDER_CONSOLE_CORE, "icons/16/report.png"));
		GridData gridData = new GridData();
		gridData.widthHint = 140;
		btnGenerateReport.setLayoutData(gridData);
		btnGenerateReport.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!validateInputs()) {
					return;
				}

				// Collect parameter values
				Map<String, Object> paramValues = new HashMap<String, Object>();
				if (paramContainer != null && paramContainer.getChildren() != null) {
					Control[] children = paramContainer.getChildren();
					for (Control control : children) {
						if (control instanceof Text) {
							Text t = (Text) control;
							String key = t.getData().toString();
							paramValues.put(key, t.getText());
						}
					}
				}

				// Send parameter values and template ID to generate report!
				ReportGenerationRequest report = new ReportGenerationRequest();
				report.setViewId(selectedView.getId());
				report.setParamValues(paramValues);

				try {
					ReportExportType type = (ReportExportType) getSelectedValue(cmbExportType);
					if (type == ReportExportType.DISPLAY_TABLE) {
						List<Object[]> list = ReportRestUtils.generateView(report);
						generateTable(list);
					} else if (type == ReportExportType.PDF_FILE) {
						byte[] pdf = ReportRestUtils.exportPdf(report);
						if (pdf == null) {
							Notifier.warning(null, Messages.getString("EMPTY_REPORT"));
							return;
						}
						final DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
						dialog.setMessage(Messages.getString("SELECT_DOWNLOAD_DIR"));
						String path = dialog.open();
						if (path == null || path.isEmpty()) {
							return;
						}
						if (!path.endsWith("/")) {
							path += "/";
						}
						// Save report
						FileOutputStream fos = new FileOutputStream(
								path + selectedView.getName() + (new Date().getTime()) + ".pdf");
						fos.write(pdf);
						fos.close();
						Notifier.success(null, Messages.getString("REPORT_SAVED"));
					}
				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		createButton(parent, IDialogConstants.CANCEL_ID, Messages.getString("CANCEL"), true);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	private Object getSelectedValue(Combo combo) {
		int selectionIndex = combo.getSelectionIndex();
		if (selectionIndex > -1 && combo.getItem(selectionIndex) != null
				&& combo.getData(selectionIndex + "") != null) {
			return combo.getData(selectionIndex + "");
		}
		return null;
	}

}
