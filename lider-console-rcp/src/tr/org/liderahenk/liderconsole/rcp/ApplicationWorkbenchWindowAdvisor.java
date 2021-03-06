package tr.org.liderahenk.liderconsole.rcp;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private IEditorPart lastActiveEditor = null;
	private IPerspectiveDescriptor lastPerspective = null;
	private IWorkbenchPage lastActivePage;
	private String lastEditorTitle = ""; //$NON-NLS-1$
	private IAdaptable lastInput;
	private IPropertyListener editorPropertyListener = new IPropertyListener() {
		public void propertyChanged(Object source, int propId) {
			if (propId == IWorkbenchPartConstants.PROP_TITLE && lastActiveEditor != null) {
				String newTitle = lastActiveEditor.getTitle();
				if (!lastEditorTitle.equals(newTitle)) {
					recomputeTitle();
				}
			}
		}
	};

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(950, 700));
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
		configurer.setShowPerspectiveBar(true);
		configurer.setShowProgressIndicator(true);
		hookTitleUpdateListeners(configurer);
	}

	@Override
	public void postWindowRestore() throws WorkbenchException {
		cleanUpEditorArea();
	}

	/**
	 * Close any empty editor stacks that may have been left open when the
	 * Workbench Window shut down.
	 * 
	 */
	@Override
	protected void cleanUpEditorArea() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		if (windows != null && windows.length > 0) {
			IWorkbenchWindow window = windows[0];
			IWorkbenchPage activePage = window.getActivePage();
			activePage.closeAllEditors(false);
		}
	}

	/**
	 * Hooks up the listeners to update the window title.
	 * 
	 * @param configurer
	 */
	private void hookTitleUpdateListeners(IWorkbenchWindowConfigurer configurer) {
		configurer.getWindow().addPageListener(new IPageListener() {
			public void pageActivated(IWorkbenchPage page) {
				updateTitle(false);
			}

			public void pageClosed(IWorkbenchPage page) {
				updateTitle(false);
			}

			public void pageOpened(IWorkbenchPage page) {
				// do nothing
			}
		});
		configurer.getWindow().addPerspectiveListener(new PerspectiveAdapter() {
			public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				updateTitle(false);
			}

			public void perspectiveSavedAs(IWorkbenchPage page, IPerspectiveDescriptor oldPerspective,
					IPerspectiveDescriptor newPerspective) {
				updateTitle(false);
			}

			public void perspectiveDeactivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				updateTitle(false);
			}
		});
		configurer.getWindow().getPartService().addPartListener(new IPartListener2() {
			public void partActivated(IWorkbenchPartReference ref) {
				if (ref instanceof IEditorReference) {
					updateTitle(false);
				}
			}

			public void partBroughtToTop(IWorkbenchPartReference ref) {
				if (ref instanceof IEditorReference) {
					updateTitle(false);
				}
			}

			public void partClosed(IWorkbenchPartReference ref) {
				updateTitle(false);
			}

			public void partDeactivated(IWorkbenchPartReference ref) {
				// do nothing
			}

			public void partOpened(IWorkbenchPartReference ref) {
				// do nothing
			}

			public void partHidden(IWorkbenchPartReference ref) {
				if (ref.getPart(false) == lastActiveEditor && lastActiveEditor != null) {
					updateTitle(true);
				}
			}

			public void partVisible(IWorkbenchPartReference ref) {
				if (ref.getPart(false) == lastActiveEditor && lastActiveEditor != null) {
					updateTitle(false);
				}
			}

			public void partInputChanged(IWorkbenchPartReference ref) {
				// do nothing
			}
		});

	}

	/**
	 * Computes the title.
	 * 
	 * @return the computed title
	 */
	private String computeTitle() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		IWorkbenchPage currentPage = configurer.getWindow().getActivePage();
		IEditorPart activeEditor = null;
		if (currentPage != null) {
			activeEditor = lastActiveEditor;
		}

		String title = ""; //$NON-NLS-1$

		if (currentPage != null) {
			if (activeEditor != null) {
				lastEditorTitle = activeEditor.getTitleToolTip();
				title = NLS.bind("{0}", lastEditorTitle); //$NON-NLS-1$
			}
			String label = Messages.ApplicationWorkbenchWindowAdvisor_LABEL;
			if (title != null && !title.equals("")) {
				title = NLS.bind("{0} - {1}", label, title); //$NON-NLS-1$
			} else {
				title = NLS.bind("{0}", label);
			}
		}

		return title;
	}

	/**
	 * Recomputes the title.
	 */
	private void recomputeTitle() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		String oldTitle = configurer.getTitle();
		String newTitle = computeTitle();
		if (!newTitle.equals(oldTitle)) {
			configurer.setTitle(newTitle);
		}
	}

	/**
	 * Updates the window title. Format will be: [pageInput -]
	 * [currentPerspective -] [editorInput -] [workspaceLocation -] productName
	 * 
	 * @param editorHidden
	 */
	private void updateTitle(boolean editorHidden) {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		IWorkbenchWindow window = configurer.getWindow();
		IEditorPart activeEditor = null;
		IWorkbenchPage currentPage = window.getActivePage();
		IPerspectiveDescriptor persp = null;
		IAdaptable input = null;

		if (currentPage != null) {
			activeEditor = currentPage.getActiveEditor();
			persp = currentPage.getPerspective();
			input = currentPage.getInput();
		}

		if (editorHidden) {
			activeEditor = null;
		}

		// Nothing to do if the editor hasn't changed
		if (activeEditor == lastActiveEditor && currentPage == lastActivePage && persp == lastPerspective
				&& input == lastInput) {
			return;
		}

		if (lastActiveEditor != null) {
			lastActiveEditor.removePropertyListener(editorPropertyListener);
		}

		lastActiveEditor = activeEditor;
		lastActivePage = currentPage;
		lastPerspective = persp;
		lastInput = input;

		if (activeEditor != null) {
			activeEditor.addPropertyListener(editorPropertyListener);
		}

		recomputeTitle();
	}

	@Override
	public void postWindowCreate() {
		super.postWindowCreate();
		hideUnusedActions();
		removeUnusedPreferences();
	}

	/**
	 * Hide unused actions contributed by org.eclipse.* plugins
	 */
	private void hideUnusedActions() {
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i = 0; i < windows.length; ++i) {
			IWorkbenchPage page = windows[i].getActivePage();
			if (page != null) {
				// hide generic 'File' commands
				page.hideActionSet("org.eclipse.ui.actionSet.openFiles");
				// hide 'Convert Line Delimiters To...'
				page.hideActionSet("org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo");
				// hide 'Search' commands
				page.hideActionSet("org.eclipse.search.searchActionSet");
				// hide 'Annotation' commands
				page.hideActionSet("org.eclipse.ui.edit.text.actionSet.annotationNavigation");
				// hide 'Forward/Back' type navigation commands
				page.hideActionSet("org.eclipse.ui.edit.text.actionSet.navigation");
			}
		}
	}

	/**
	 * Hides unwanted UI items contributed by org.eclipse.* plugins
	 */
	private void removeUnusedPreferences() {
		PreferenceManager manager = PlatformUI.getWorkbench().getPreferenceManager();
		if (manager != null) {
			// Remove Debug preferences page
			manager.remove("org.eclipse.debug.ui.DebugPreferencePage");
			// Remove Team preferences page
			manager.remove("org.eclipse.team.ui.TeamPreferences");
			// Remove General Eclipse preferences page
			manager.remove("org.eclipse.ui.preferencePages.Workbench");
		}
	}

}
