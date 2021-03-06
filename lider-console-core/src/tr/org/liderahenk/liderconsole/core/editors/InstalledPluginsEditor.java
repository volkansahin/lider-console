package tr.org.liderahenk.liderconsole.core.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.liderconsole.core.editorinput.DefaultEditorInput;
import tr.org.liderahenk.liderconsole.core.i18n.Messages;
import tr.org.liderahenk.liderconsole.core.model.Plugin;
import tr.org.liderahenk.liderconsole.core.rest.utils.PluginRestUtils;
import tr.org.liderahenk.liderconsole.core.utils.SWTResourceManager;
import tr.org.liderahenk.liderconsole.core.widgets.Notifier;

/**
 * Editor class for displaying plugins installed on Lider.
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
 *
 */
public class InstalledPluginsEditor extends EditorPart {

	private static final Logger logger = LoggerFactory.getLogger(InstalledPluginsEditor.class);

	private Text txtSearch;
	private TableViewer tableViewer;
	private TableFilter tableFilter;

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		setPartName(((DefaultEditorInput) input).getLabel());
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, GridData.FILL);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(1, false));
		createTableArea(composite);
	}

	/**
	 * Create main widget of the editor - table viewer.
	 * 
	 * @param composite
	 */
	private void createTableArea(final Composite parent) {

		createTableFilterArea(parent);

		tableViewer = SWTResourceManager.createTableViewer(parent);
		createTableColumns();
		populateTable();

		tableFilter = new TableFilter();
		tableViewer.addFilter(tableFilter);
		tableViewer.refresh();
	}

	/**
	 * Create table filter area
	 * 
	 * @param parent
	 */
	private void createTableFilterArea(Composite parent) {
		Composite filterContainer = new Composite(parent, SWT.NONE);
		filterContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		filterContainer.setLayout(new GridLayout(2, false));

		// Search label
		Label lblSearch = new Label(filterContainer, SWT.NONE);
		lblSearch.setFont(SWTResourceManager.getFont("Sans", 9, SWT.BOLD));
		lblSearch.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		lblSearch.setText(Messages.getString("SEARCH_FILTER"));

		// Filter table rows
		txtSearch = new Text(filterContainer, SWT.BORDER | SWT.SEARCH);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.setToolTipText(Messages.getString("SEARCH_PLUGIN_TOOLTIP"));
		txtSearch.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				tableFilter.setSearchText(txtSearch.getText());
				tableViewer.refresh();
			}
		});
	}

	/**
	 * Apply filter to table rows. (Search text can be plugin name or version)
	 *
	 */
	public class TableFilter extends ViewerFilter {

		private String searchString;

		public void setSearchText(String s) {
			this.searchString = ".*" + s + ".*";
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (searchString == null || searchString.length() == 0) {
				return true;
			}
			Plugin plugin = (Plugin) element;
			return plugin.getName().matches(searchString) || plugin.getVersion().matches(searchString);
		}
	}

	/**
	 * Create table columns related to policy database columns.
	 * 
	 */
	private void createTableColumns() {

		// Plugin name
		TableViewerColumn pluginNameColumn = SWTResourceManager.createTableViewerColumn(tableViewer,
				Messages.getString("PLUGIN_NAME"), 200);
		pluginNameColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plugin) {
					return Messages.getString(((Plugin) element).getName());
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Plugin version
		TableViewerColumn pluginVersionColumn = SWTResourceManager.createTableViewerColumn(tableViewer,
				Messages.getString("PLUGIN_VERSION"), 150);
		pluginVersionColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plugin) {
					return ((Plugin) element).getVersion();
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Description
		TableViewerColumn descriptionColumn = SWTResourceManager.createTableViewerColumn(tableViewer,
				Messages.getString("DESCRIPTION"), 100);
		descriptionColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plugin) {
					return ((Plugin) element).getDescription();
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Create date
		TableViewerColumn createDateColumn = SWTResourceManager.createTableViewerColumn(tableViewer,
				Messages.getString("CREATE_DATE"), 150);
		createDateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plugin) {
					return ((Plugin) element).getCreateDate() != null ? ((Plugin) element).getCreateDate().toString()
							: Messages.getString("UNTITLED");
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Machine-oriented
		TableViewerColumn machineOrientedColumn = SWTResourceManager.createTableViewerColumn(tableViewer,
				Messages.getString("MACHINE_ORIENTED_PLUGIN"), 100);
		machineOrientedColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plugin) {
					return ((Plugin) element).isMachineOriented() ? Messages.getString("YES")
							: Messages.getString("NO");
				}
				return Messages.getString("UNTITLED");
			}
		});

		// User-oriented
		TableViewerColumn userOrientedColumn = SWTResourceManager.createTableViewerColumn(tableViewer,
				Messages.getString("USER_ORIENTED_PLUGIN"), 100);
		userOrientedColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plugin) {
					return ((Plugin) element).isUserOriented() ? Messages.getString("YES") : Messages.getString("NO");
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Policy plugin
		TableViewerColumn policyPluginColumn = SWTResourceManager.createTableViewerColumn(tableViewer,
				Messages.getString("POLICY_PLUGIN"), 100);
		policyPluginColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plugin) {
					return ((Plugin) element).isPolicyPlugin() ? Messages.getString("YES") : Messages.getString("NO");
				}
				return Messages.getString("UNTITLED");
			}
		});

		// Task plugin
		TableViewerColumn taskPluginColumn = SWTResourceManager.createTableViewerColumn(tableViewer,
				Messages.getString("TASK_PLUGIN"), 100);
		taskPluginColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Plugin) {
					return ((Plugin) element).isTaskPlugin() ? Messages.getString("YES") : Messages.getString("NO");
				}
				return Messages.getString("UNTITLED");
			}
		});

	}

	/**
	 * Search plugins by plugin name and version, then populate specified table
	 * with plugin records.
	 * 
	 */
	private void populateTable() {
		try {
			List<Plugin> plugins = PluginRestUtils.list(null, null);
			tableViewer.setInput(plugins != null ? plugins : new ArrayList<Plugin>());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			Notifier.error(null, Messages.getString("ERROR_ON_LIST"));
		}
	}

	@Override
	public void setFocus() {
	}

}
