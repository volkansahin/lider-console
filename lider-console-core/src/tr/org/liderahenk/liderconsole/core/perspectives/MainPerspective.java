package tr.org.liderahenk.liderconsole.core.perspectives;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class MainPerspective implements IPerspectiveFactory {

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		layout.setFixed(true);
		// This method can be used to add views
		// But no need to implement it here since we can use plugin.xml file to
		// create views
	}

}
