package tr.org.liderahenk.liderconsole.core.ldap.listeners;

import java.util.Map;

import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.studio.common.core.jobs.StudioProgressMonitor;
import org.apache.directory.studio.connection.core.Connection;
import org.apache.directory.studio.connection.core.ConnectionParameter.AuthenticationMethod;
import org.apache.directory.studio.connection.core.IConnectionListener;
import org.apache.directory.studio.connection.core.io.StudioNamingEnumeration;
import org.apache.directory.studio.connection.core.jobs.CloseConnectionsRunnable;
import org.apache.directory.studio.connection.core.jobs.StudioConnectionJob;
import org.apache.directory.studio.ldapbrowser.ui.views.browser.BrowserView;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.liderconsole.core.config.ConfigProvider;
import tr.org.liderahenk.liderconsole.core.constants.LiderConstants;
import tr.org.liderahenk.liderconsole.core.current.RestSettings;
import tr.org.liderahenk.liderconsole.core.current.UserSettings;
import tr.org.liderahenk.liderconsole.core.editorinput.DefaultEditorInput;
import tr.org.liderahenk.liderconsole.core.i18n.Messages;
import tr.org.liderahenk.liderconsole.core.ldap.utils.LdapUtils;
import tr.org.liderahenk.liderconsole.core.rest.responses.IResponse;
import tr.org.liderahenk.liderconsole.core.rest.utils.TaskRestUtils;
import tr.org.liderahenk.liderconsole.core.widgets.Notifier;
import tr.org.liderahenk.liderconsole.core.xmpp.XMPPClient;

/**
 * This class listens to LDAP connection & send events accordingly.
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
 *
 */
public class LdapConnectionListener implements IConnectionListener {

	private static final Logger logger = LoggerFactory.getLogger(LdapConnectionListener.class);

	private final IEventBroker eventBroker = (IEventBroker) PlatformUI.getWorkbench().getService(IEventBroker.class);

	private static Connection conn;
	private static StudioProgressMonitor monitor;

	public LdapConnectionListener() {

		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		if (windows != null && windows.length > 0) {

			IWorkbenchWindow window = windows[0];

			// This code block is used to paint initially 'offline' icons on
			// LDAP user and agent entries.
			BrowserView browserView = (BrowserView) window.getActivePage().findView(LiderConstants.VIEWS.BROWSER_VIEW);
			if (browserView != null) {

				final Tree tree = browserView.getMainWidget().getViewer().getTree();
				final TreePaintListener paintListener = new TreePaintListener(tree);

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						tree.addListener(SWT.MeasureItem, paintListener);
						tree.addListener(SWT.PaintItem, paintListener);
						tree.addListener(SWT.EraseItem, paintListener);
					}
				});
			}
		}
	}

	@Override
	public void connectionClosed(Connection conn, StudioProgressMonitor mon) {
		closeAllEditors();

		XMPPClient.getInstance().disconnect();

		RestSettings.setServerUrl(null);
		UserSettings.setCurrentUserDn(null);
		UserSettings.setCurrentUserPassword(null);
		UserSettings.setCurrentUserPrivileges(null);

		eventBroker.send("check_lider_status", null);

		LdapConnectionListener.conn = null;
		if (monitor != null) {
			monitor.done();
			monitor = null;
		}
	}

	/**
	 * Close all opened editors in a safe manner.
	 */
	private void closeAllEditors() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
					if (windows != null && windows.length > 0) {
						IWorkbenchWindow window = windows[0];
						IWorkbenchPage activePage = window.getActivePage();
						activePage.closeAllEditors(false);
					}
					Notifier.success(null, Messages.getString("LIDER_CONNECTION_CLOSED"));
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
	}

	@Override
	public void connectionOpened(Connection conn, StudioProgressMonitor mon) {

		monitor = new StudioProgressMonitor(mon);

		Connection connWillBeClosed = LdapConnectionListener.conn;
		LdapConnectionListener.conn = conn;

		String baseDn = LdapUtils.getInstance().findBaseDn(conn);
		if (baseDn == null || baseDn.equals("")) {
			Notifier.error(null, "LDAP_BASE_DN_ERROR");
			return;
		}

		try {
			// Set the application-wide current user.
			AuthenticationMethod authMethod = conn.getAuthMethod();
			if (authMethod.equals(AuthenticationMethod.SASL_CRAM_MD5)
					|| authMethod.equals(AuthenticationMethod.SASL_DIGEST_MD5)) {
				String uid = conn.getBindPrincipal();
				String principal = LdapUtils.getInstance().findDnByUid(uid, conn, monitor);
				String passwd = conn.getBindPassword();
				UserSettings.setCurrentUserDn(principal);
				UserSettings.setCurrentUserId(uid);
				UserSettings.setCurrentUserPassword(passwd);
			} else {
				String principal = conn.getBindPrincipal();
				String uid = LdapUtils.getInstance().findAttributeValueByDn(principal,
						ConfigProvider.getInstance().get(LiderConstants.CONFIG.USER_LDAP_UID_ATTR), conn, monitor);
				String passwd = conn.getBindPassword();
				UserSettings.setCurrentUserDn(principal);
				UserSettings.setCurrentUserId(uid);
				UserSettings.setCurrentUserPassword(passwd);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			Notifier.error(null, "LDAP_USER_CREDENTIALS_ERROR");
			return;
		}

		if ("".equals(UserSettings.USER_DN)) {
			Notifier.error(null, Messages.getString("LDAP_USER_MISSING_UID_ERROR",
					ConfigProvider.getInstance().get(LiderConstants.CONFIG.USER_LDAP_UID_ATTR)));
			return;
		}

		Map<String, Map<String, Boolean>> privileges = LdapUtils.getInstance().findPrivileges(conn, monitor);
		UserSettings.setCurrentUserPrivileges(privileges);
		String configDn = ConfigProvider.getInstance().get(LiderConstants.CONFIG.CONFIG_LDAP_DN_PREFIX) + "," + baseDn;

		StudioNamingEnumeration configEntries = LdapUtils.getInstance().search(configDn, LdapUtils.OBJECT_CLASS_FILTER,
				new String[] {}, SearchControls.OBJECT_SCOPE, 1, conn, monitor);
		try {
			if (configEntries != null && configEntries.hasMore()) {
				SearchResult item = configEntries.next();

				// REST Address
				Attribute attribute = item.getAttributes()
						.get(ConfigProvider.getInstance().get(LiderConstants.CONFIG.LDAP_REST_ADDRESS_ATTR));
				String restFulAddress = LdapUtils.getInstance().findAttributeValue(attribute);

				if (restFulAddress != null && !restFulAddress.isEmpty()) {

					// TODO we should set this after reading system configs
					// that way we can ensure that both LDAP and Lider
					// connection established successfully!
					RestSettings.setServerUrl(restFulAddress);
					IResponse response = null;

					try {
						response = TaskRestUtils.execute("LIDER-CONFIG", "1.0.0", "GET-SYSTEM-CONFIG", true);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
						Notifier.error(null, Messages.getString("REST_SERVER_ACCESS_ERROR", restFulAddress));
						Notifier.error(null, Messages.getString("CHECK_LIDER_STATUS_AND_REST_SERVICE"));
						return;
					}
					if (response != null) {
						Map<String, Object> config = response.getResultMap();
						if (config != null) {
							// Initialise UID map before connecting to
							// XMPP server.
							LdapUtils.getInstance().getUidMap(conn, monitor);
							try {
								XMPPClient.getInstance().connect(UserSettings.USER_ID, UserSettings.USER_PASSWORD,
										config.get("xmppServiceName").toString(), config.get("xmppHost").toString(),
										new Integer(config.get("xmppPort").toString()));
								Notifier.success(null, Messages.getString("LIDER_CONNECTION_OPENED"));
							} catch (Exception e) {
								logger.error(e.getMessage(), e);
								Notifier.error(null, Messages.getString("XMPP_CONNECTION_ERROR") + "\n" + Messages.getString("CHECK_XMPP_SERVER"));
								return;
							}
						} else {
							Notifier.error(null, Messages.getString("XMPP_CONNECTION_ERROR"));
						}
					}
				} else {
					Notifier.error(null, Messages.getString("LIDER_CONDIG_DN_ERROR", configDn));
				}
			}

			openLdapSearchEditor();

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		eventBroker.send("check_lider_status", null);

		// Close previous connection if it was opened.
		if (connWillBeClosed != null && connWillBeClosed.getConnectionWrapper().isConnected()) {
			new StudioConnectionJob(new CloseConnectionsRunnable(connWillBeClosed)).execute();
		}
	}

	private void openLdapSearchEditor() {
		// Open LDAP Search by default editor on startup
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		if (windows != null && windows.length > 0) {
			IWorkbenchWindow window = windows[0];
			final IWorkbenchPage activePage = window.getActivePage();
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					try {
						activePage.openEditor(new DefaultEditorInput(Messages.getString("LDAP_SEARCH")),
								LiderConstants.EDITORS.LDAP_SEARCH_EDITOR);
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	public static Connection getConnection() {
		return conn;
	}

	public static StudioProgressMonitor getMonitor() {
		return monitor;
	}

}
