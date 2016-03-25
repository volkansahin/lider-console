package tr.org.liderahenk.liderconsole.core.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tr.org.liderahenk.liderconsole.core.config.ConfigProvider;
import tr.org.liderahenk.liderconsole.core.constants.LiderConstants;
import tr.org.liderahenk.liderconsole.core.current.RestSettings;
import tr.org.liderahenk.liderconsole.core.current.UserSettings;
import tr.org.liderahenk.liderconsole.core.i18n.Messages;
import tr.org.liderahenk.liderconsole.core.rest.requests.IRequest;
import tr.org.liderahenk.liderconsole.core.rest.responses.IResponse;
import tr.org.liderahenk.liderconsole.core.rest.responses.RestResponse;
import tr.org.liderahenk.liderconsole.core.rest.utils.PolicyUtils;
import tr.org.liderahenk.liderconsole.core.rest.utils.ProfileUtils;
import tr.org.liderahenk.liderconsole.core.rest.utils.TaskUtils;
import tr.org.liderahenk.liderconsole.core.widgets.Notifier;

/**
 * RestClient provides utility methods for sending requests to Lider Server and
 * handling their responses. Instead of this class, it is recommended that
 * plugin developers should use {@link ProfileUtils}, {@link PolicyUtils} or
 * {@link TaskUtils} according to their needs.
 * 
 * @author <a href="mailto:emre.akkaya@agem.com.tr">Emre Akkaya</a>
 *
 */
@SuppressWarnings("restriction")
public class RestClient {

	private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

	/**
	 * Content type header
	 */
	private static final String CONTENT_TYPE_HEADER = "Content-Type";

	/**
	 * Accept header
	 */
	private static final String ACCEPT_HEADER = "Accept";

	/**
	 * Use only JSON for requests
	 */
	private static final String ACCEPT_MIME_TYPE = "application/json";
	private static final String CONTENT_MIME_TYPE = "application/json; charset=UTF-8";

	/**
	 * Username header
	 */
	private static final String USERNAME_HEADER = "username";

	/**
	 * Password header
	 */
	private static final String PASSWORD_HEADER = "password";

	/**
	 * Define this as a global variable to overcome re-generating JSessionId for
	 * each request.
	 */
	private static HttpClient httpClient = null;

	static {
		RequestConfig config = RequestConfig.custom()
				.setConnectionRequestTimeout(
						ConfigProvider.getInstance().getInt(LiderConstants.CONFIG.REST_CONN_REQUEST_TIMEOUT))
				.setConnectTimeout(ConfigProvider.getInstance().getInt(LiderConstants.CONFIG.REST_CONNECT_TIMEOUT))
				.setSocketTimeout(ConfigProvider.getInstance().getInt(LiderConstants.CONFIG.REST_SOCKET_TIMEOUT))
				.build();
		httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
	}

	/**
	 * Response object that is used inside progress services
	 */
	private static IResponse response = null;

	private RestClient() {
	}

	/**
	 * Main method that can be used to send POST requests to Lider Server.
	 * 
	 * @param request
	 * @return an instance of RestResponse if successful, null otherwise.
	 * @throws Exception
	 * @throws UnsupportedEncodingException
	 */
	public static IResponse post(final IRequest request, final String url) throws Exception {

		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		progressService.runInUI(progressService, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				monitor.beginTask(Messages.getString("SENDING_REQUEST"), 100);
				response = null;

				try {
					HttpPost httpPost = new HttpPost(buildUrl(url));
					httpPost.setHeader(CONTENT_TYPE_HEADER, CONTENT_MIME_TYPE);
					httpPost.setHeader(ACCEPT_HEADER, ACCEPT_MIME_TYPE);

					// Convert IRequest instance to JSON and pass as HttpEntity
					StringEntity entity = new StringEntity(URLEncoder.encode(request.toJson(), "UTF-8"),
							StandardCharsets.UTF_8);
					entity.setContentEncoding("UTF-8");
					entity.setContentType(CONTENT_MIME_TYPE);
					httpPost.setEntity(entity);

					httpPost.setHeader(USERNAME_HEADER, UserSettings.USER_ID);
					httpPost.setHeader(PASSWORD_HEADER, UserSettings.USER_PASSWORD);

					monitor.worked(20);

					HttpResponse httpResponse = httpClient.execute(httpPost);
					if (httpResponse.getStatusLine().getStatusCode() != 200) {
						logger.warn("REST failure. Status code: {} Reason: {} ",
								new Object[] { httpResponse.getStatusLine().getStatusCode(),
										httpResponse.getStatusLine().getReasonPhrase() });
					} else { // Status OK

						BufferedReader bufferredReader = new BufferedReader(
								new InputStreamReader(httpResponse.getEntity().getContent()));
						StringBuilder buffer = new StringBuilder();
						String line;
						while ((line = bufferredReader.readLine()) != null) {
							buffer.append(line);
						}

						response = new ObjectMapper().readValue(buffer.toString(), RestResponse.class);
					}

					if (response != null) {
						logger.debug("Response received: {}", response);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					Notifier.error(null, Messages.getString("ERROR_ON_REQUEST"));
				}

				monitor.worked(100);
				monitor.done();
			}
		}, null);

		return response;
	}

	/**
	 * Main method that can be used to send GET requests to Lider server.
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static IResponse get(final String url) throws Exception {

		IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
		progressService.runInUI(progressService, new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				monitor.beginTask(Messages.getString("SENDING_REQUEST"), 100);
				response = null;

				try {
					HttpGet httpGet = new HttpGet(buildUrl(url));
					httpGet.setHeader(CONTENT_TYPE_HEADER, CONTENT_MIME_TYPE);
					httpGet.setHeader(ACCEPT_HEADER, ACCEPT_MIME_TYPE);

					HttpResponse httpResponse = httpClient.execute(httpGet);
					if (httpResponse.getStatusLine().getStatusCode() != 200) {
						logger.warn("REST failure. Status code: {} Reason: {} ",
								new Object[] { httpResponse.getStatusLine().getStatusCode(),
										httpResponse.getStatusLine().getReasonPhrase() });
					} else {

						BufferedReader bufferredReader = new BufferedReader(
								new InputStreamReader(httpResponse.getEntity().getContent()));
						StringBuilder buffer = new StringBuilder();
						String line;
						while ((line = bufferredReader.readLine()) != null) {
							buffer.append(line);
						}

						response = new ObjectMapper().readValue(buffer.toString(), RestResponse.class);
					}

					if (response != null) {
						logger.debug("Response received: {}", response);
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					Notifier.error(null, Messages.getString("ERROR_ON_REQUEST"));
				}

				monitor.worked(100);
				monitor.done();
			}
		}, null);

		return response;
	}

	/**
	 * Builds REST URL based on provided parameters. Resulting URL made of this
	 * format:<br/>
	 * {SERVER_IP}/{BASE_URL}/{ACTION_URL}<br/>
	 * 
	 * @param resource
	 * @param action
	 * @return
	 */
	private static String buildUrl(String base) {
		String tmp = RestSettings.getServerUrl();
		// Handle trailing/leading slash characters.
		if (!tmp.endsWith("/") && !base.startsWith("/")) {
			tmp = tmp + "/" + base;
		} else if (tmp.endsWith("/") && base.startsWith("/")) {
			tmp = tmp + base.substring(1);
		} else {
			tmp = tmp + base;
		}
		return tmp;
	}

}
