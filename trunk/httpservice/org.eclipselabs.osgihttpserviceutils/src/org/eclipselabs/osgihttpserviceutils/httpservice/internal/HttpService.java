package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpRequestInterceptor;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServer;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.eclipselabs.osgihttpserviceutils.httpservice.RequestContext;
import org.eclipselabs.osgihttpserviceutils.httpservice.RequestService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component(immediate = true)
public class HttpService implements HttpAdminService, RequestService {

	static class DefaultHttpServerInstance implements HttpServerInstance {

		private HttpServerManager httpServerManager;

		private ServiceRegistration httpServiceRegistration;

		public void setHttpServerManager(HttpServerManager httpServerManager) {
			this.httpServerManager = httpServerManager;
		}

		public void setHttpServiceRegistration(
				ServiceRegistration httpServiceRegistration) {
			this.httpServiceRegistration = httpServiceRegistration;
		}

		@Override
		public void shutdown() {
			if (httpServiceRegistration != null) {
				httpServiceRegistration.unregister();
				setHttpServiceRegistration(null);
			}
			if (httpServerManager != null) {
				try {
					httpServerManager.shutdown();
				} catch (Exception exp) {
					throw new HttpServiceInternalException(exp);
				}
				setHttpServerManager(null);
			}
		}

		@Override
		public void reloadConfiguration() {
			throw new UnsupportedOperationException();
		}

	}
	
	final static String PROPERTY_PREFIX = "org.eclipselabs.osgihttpserviceutils.httpservice."; //$NON-NLS-1$
	
	static final String DEFAULT_PID = "default"; //$NON-NLS-1$

	private static final Logger LOG = LoggerFactory.getLogger(HttpService.class);

	private static final String LOG_STDERR_THRESHOLD = "org.eclipse.equinox.http.jetty.log.stderr.threshold"; //$NON-NLS-1$

	private static final String PROP_ORG_OSGI_SERVICE_HTTP = "org.osgi.service.http.";

	private static final String PROP_PORT = ".port";

	private BundleContext context;

	private final List<HttpServerInstance> httpServerInstances = new ArrayList<HttpServerInstance>();

	private File jettyWorkDir;
	
	private final DefaultRequestContext requestContext = new DefaultRequestContext();

	private final List<HttpRequestInterceptor> requestInterceptors = new LinkedList<HttpRequestInterceptor>();

	private final Map<String, DefaultHttpServer> servers = new HashMap<String, DefaultHttpServer>();

	public HttpService() {
	}

	@Activate
	public void activate(BundleContext context) throws Exception {
		setStdErrLogThreshold(context.getProperty(LOG_STDERR_THRESHOLD));
		this.context = context;
	}

	@Reference(unbind = "removeRequestInterceptors", multiple = true, optional = true, dynamic = true)
	public void addRequestInterceptors(HttpRequestInterceptor interceptor) {
		 requestInterceptors.add(interceptor);
	}

	protected Dictionary<Object, Object> createDefaultSettings(
			BundleContext context) {
		Dictionary<Object, Object> defaultSettings = new Hashtable<Object, Object>();

		// PID
		defaultSettings.put(Constants.SERVICE_PID, DEFAULT_PID);

		// HTTP Enabled (default is true)
		String httpEnabledProperty = context.getProperty(PROPERTY_PREFIX
				+ JettyConstants.HTTP_ENABLED);

		Boolean httpEnabled = null;
		if (httpEnabledProperty == null) {
			httpEnabled = Boolean.TRUE;
		} else {
			httpEnabled = Boolean.valueOf(httpEnabledProperty);
		}
		defaultSettings.put(JettyConstants.HTTP_ENABLED, httpEnabled);

		// HTTP Host (default is 0.0.0.0)
		String httpHost = context.getProperty(PROPERTY_PREFIX
				+ JettyConstants.HTTP_HOST);
		if (httpHost != null) {
			defaultSettings.put(JettyConstants.HTTP_HOST, httpHost);
		}

		// HTTPS Enabled (default is false)
		Boolean httpsEnabled = Boolean.valueOf(context
				.getProperty(PROPERTY_PREFIX
				+ JettyConstants.HTTPS_ENABLED));
		defaultSettings.put(JettyConstants.HTTPS_ENABLED, httpsEnabled);

		// Servlet Context Path
		String contextpath = context.getProperty(PROPERTY_PREFIX
				+ JettyConstants.CONTEXT_PATH);
		if (contextpath != null) {
			defaultSettings.put(JettyConstants.CONTEXT_PATH, contextpath);
		}

		// Session Inactive Interval (timeout)
		String sessionInactiveInterval = context.getProperty(PROPERTY_PREFIX
				+ JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL);
		if (sessionInactiveInterval != null) {
			try {
				defaultSettings.put(
						JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL,
						Integer.valueOf(sessionInactiveInterval));
			} catch (NumberFormatException e) {
				// (log this) ignore
			}
		}

		// Other Info
		String otherInfo = context.getProperty(PROPERTY_PREFIX
				+ JettyConstants.OTHER_INFO);
		if (otherInfo != null) {
			defaultSettings.put(JettyConstants.OTHER_INFO, otherInfo);
		}

		return defaultSettings;
	}

	@Override
	public HttpServer createHttpServer(String symbolicName) {
		final String method = "createHttpServer(): ";
		if(servers.containsKey(symbolicName)){
			LOG.warn(method
					+ "server with symbolic name {} is already running!",
					symbolicName);
			return servers.get(symbolicName);
		}
		DefaultHttpServer httpServer = new DefaultHttpServer(symbolicName) {
			@Override
			public HttpServerInstance start() {
				return startServer(this);
			}
		};
		servers.put(symbolicName, httpServer);
		return httpServer;
	}

	@Deactivate
	public void deactivate(BundleContext context) throws Exception {
		this.context = null;
		ArrayList<HttpServerInstance> shutdownServers = new ArrayList<HttpServerInstance>();
		for (HttpServerInstance instance : getHttpServerInstances()) {
			shutdownServers.add(instance);
		}
		for (HttpServerInstance httpServerInstance : shutdownServers) {
			httpServerInstance.shutdown();
		}
	}

	private int getPort(HttpServer server) {
		final String method = "getPort(): ";
		int port = server.getPort();
		String name = server.getSymbolicName();
		if (port <= 0) {
			String systemPropertyName = PROP_ORG_OSGI_SERVICE_HTTP + name
					+ PROP_PORT;
			LOG.debug(method
					+ "Port is not set look up port system property : {}",
					systemPropertyName);
			String strPort = System.getProperty(systemPropertyName);
			if (strPort != null) {
				LOG.debug(method
						+ "The port of the http server {} is set to {}", name,
						strPort);
				return Integer.valueOf(strPort);
			}
			LOG.error(
					method
							+ "No HTTP port is configured for the HTTP server {}, "
							+ "please setup a port, direct see org.eclipselabs.osgihttpserviceutils.httpservice.HttpServer, "
							+ "or set the system property {}, "
							+ "or add a jetty XML configuration for the server !!!",
					name, systemPropertyName);
			throw new HttpServiceInternalException(
					"No HTTP Port is configured for the server " + name);
		}
		LOG.debug(method + "The port of the http server {} is {}", name,
				port);
		return port;
	}

	@Override
	public RequestContext getRequestContext() {
		return requestContext;
	}

	public void removeRequestInterceptors(HttpRequestInterceptor interceptor) {
		requestInterceptors.remove(interceptor);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setStdErrLogThreshold(String property) {
		try {
			Class clazz = Class.forName("org.slf4j.Logger");
			Method method = clazz.getMethod("setThresholdLogger",
					new Class[] { String.class });
			method.invoke(null, new Object[] { property });
		} catch (Throwable t) {
			// ignore
		}
	}

	public DefaultHttpServerInstance startServer(final HttpServer httpServer) {
		HttpServerManager httpServerManager = createHttpServerManager();
		Dictionary<Object, Object> settings = createDefaultSettings(context);
		settings.put("HTTP_SERVER_CUSTOM_SERVICE_PROPS", httpServer.getServiceProperties());
		if (JettyConfigurationUtils.existsJettyXmlConfiguration(httpServer)) {
			settings.put(JettyConstants.JETTY_XML_CONFIGURATION, true);
		}
		else{
			settings.put(JettyConstants.HTTP_PORT, getPort(httpServer));
		}
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		try {
			httpServerManager.updated(DEFAULT_PID, settings);
		} catch (ConfigurationException exp) {
			throw new HttpServiceInternalException(exp);
		}
		
		Dictionary<Object, Object> serviceProps = new Hashtable<Object, Object>();
		ServiceRegistration httpServiceRegistration = context.registerService(
				ManagedServiceFactory.class.getName(), httpServerManager,
				serviceProps);
		DefaultHttpServerInstance instance = new DefaultHttpServerInstance() {
			@Override
			public void shutdown() {
				super.shutdown();
				servers.remove(httpServer.getSymbolicName());
				getHttpServerInstances().remove(this);
			};
		};
		instance.setHttpServerManager(httpServerManager);
		instance.setHttpServiceRegistration(httpServiceRegistration);
		getHttpServerInstances().add(instance);
		return instance;
	}

	protected HttpServerManager createHttpServerManager() {
		return new HttpServerManager(requestInterceptors, jettyWorkDir, requestContext);
	}

	public List<HttpServerInstance> getHttpServerInstances() {
		return httpServerInstances;
	}

	public List<HttpRequestInterceptor> getRequestInterceptors() {
		return requestInterceptors;
	}

}
