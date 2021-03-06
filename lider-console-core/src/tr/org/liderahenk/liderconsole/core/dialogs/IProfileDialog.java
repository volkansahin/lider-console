package tr.org.liderahenk.liderconsole.core.dialogs;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;

import tr.org.liderahenk.liderconsole.core.exceptions.ValidationException;
import tr.org.liderahenk.liderconsole.core.model.Profile;

/**
 * Any plugin providing implementation of this interface will automatically have
 * profile editor which has profile CRUD capabilities.
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
 *
 */
public interface IProfileDialog {

	/**
	 * This method can be used to initialize some objects if necessary.
	 * Triggered on dialog instance creation.
	 */
	void init();

	
	/**
	 * This is the main method that can be used to provide profile specific
	 * input widgets.
	 * 
	 * @param profile
	 * @param composite
	 *            parent composite instance with one-column grid layout.
	 */
	void createDialogArea(Composite parent, Profile profile);

	
	/**
	 * Triggered on 'OK' button pressed. Implementation of this method provide
	 * necessary profile data that need to be saved on database.
	 * 
	 * @return
	 * @throws Exception
	 */
	Map<String, Object> getProfileData() throws Exception;
	
	
	/**
	 * Triggered on 'OK' button pressed. Before saving profile data on database.
	 * 
	 * If validation fails for any of profile data, this method should throws a {@link ValidationException}.
	 * 
	 * @return
	 * @throws ValidationException
	 */
	void validateBeforeSave() throws ValidationException;

}
