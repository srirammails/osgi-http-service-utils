package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.equinox.http.servlet.HttpServiceServlet;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpRequestInterceptor;
import org.eclipselabs.osgihttpserviceutils.httpservice.JettyCustomizer;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.HashSessionManager;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.SessionHandler;
import org.mortbay.xml.XmlConfiguration;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class HttpServerManager implements ManagedServiceFactory {

	/**
	 * Internal {@link Servlet} implementation
	 */
	public static class InternalHttpServiceServlet implements Servlet {
		private static final long serialVersionUID = 7477982882399972088L;

		private ClassLoader contextLoader;

		final Servlet httpServiceServlet = new HttpServiceServlet();

		final DefaultRequestContext requestContext;

		final List<HttpRequestInterceptor> requestInterceptors;

		/**
		 * Creates a new {@link InternalHttpServiceServlet}
		 * 
		 * @param requestContextServiceImpl
		 *            the {@link RequestContextServiceImpl}
		 * @param requestInterceptors
		 *            a list of {@link HttpRequestInterceptor}s
		 */
		public InternalHttpServiceServlet(DefaultRequestContext requestContext,
				List<HttpRequestInterceptor> requestInterceptors) {
			this.requestInterceptors = requestInterceptors;
			this.requestContext = requestContext;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void destroy() {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(this.contextLoader);
			try {
				this.httpServiceServlet.destroy();
			} finally {
				thread.setContextClassLoader(current);
			}
			this.contextLoader = null;
		}

		@Override
		public ServletConfig getServletConfig() {
			return this.httpServiceServlet.getServletConfig();
		}

		@Override
		public String getServletInfo() {
			return this.httpServiceServlet.getServletInfo();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void init(ServletConfig config) throws ServletException {
			ServletContext context = config.getServletContext();
			this.contextLoader = (ClassLoader) context
					.getAttribute(INTERNAL_CONTEXT_CLASSLOADER);

			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(this.contextLoader);
			try {
				this.httpServiceServlet.init(config);
			} finally {
				thread.setContextClassLoader(current);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
			Thread thread = Thread.currentThread();
			ClassLoader current = thread.getContextClassLoader();
			thread.setContextClassLoader(this.contextLoader);
			requestContext.reset();
			requestContext.setRequest(req);
			requestContext.setResponse(res);
			try {
				try {
					if (requestInterceptors != null) {
						for (HttpRequestInterceptor interceptor : requestInterceptors) {
							try {
								interceptor.beforeRequest();
							} catch (Exception exp) {
								LOG.warn("A exception was thrown on invoking a request interceptor", exp);
								throw new HttpServiceInternalException(exp);
							}
						}
					}
				} catch (Exception exp) {
					LOG.warn("A exception was thrown on invoking a request interceptor", exp);
					throw new HttpServiceInternalException(exp);
				}
				try {
					this.httpServiceServlet.service(req, res);
				} finally {
					thread.setContextClassLoader(current);
				}
				try {
					if (this.requestInterceptors != null) {
						for (HttpRequestInterceptor interceptor : this.requestInterceptors) {
							try {
								interceptor.afterRequest();
							} catch (Exception exp) {
								LOG.warn("A exception was thrown on invoking a interceptor after request.", exp);
								throw new HttpServiceInternalException(exp);
							}
						}
					}
				} catch (Exception exp) {
					LOG.warn("A exception was thrown on invoking a interceptor after request.", exp);
					throw new HttpServiceInternalException(exp);
				}
			} finally {
				requestContext.reset();
			}
		}
	}

	private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir"; //$NON-NLS-1$

	private static final String DIR_PREFIX = "pid_"; //$NON-NLS-1$

	private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader"; //$NON-NLS-1$

	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServerManager.class);

	// deleteDirectory is a convenience method to recursively delete a
	// directory
	private static boolean deleteDirectory(File directory) {
		final String method = "deleteDirectory(): ";
		if (directory.exists() && directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					if (files[i].delete()) {
						LOG.debug(method + "delete file {}.",
								files[i].getAbsolutePath());
					} else {
						LOG.debug(method + "can't delete the file {}.",
								files[i].getAbsolutePath());
					}
				}
			}
		}
		return directory.delete();
	}

	private final DefaultRequestContext requestContext;

	private final List<HttpRequestInterceptor> requestInterceptors;

	@SuppressWarnings("rawtypes")
	private final Map servers = new HashMap();

	private final File workDir;

	/**
	 * Creates a new {@link HttpServerManager}
	 * 
	 * @param requestContextServiceImpl
	 *            a {@link RequestContextServiceImpl}
	 * @param requestInterceptors
	 *            a list of {@link HttpRequestInterceptor}s
	 * @param workDir
	 *            the working directory
	 */
	public HttpServerManager(List<HttpRequestInterceptor> requestInterceptors,
			File workDir, DefaultRequestContext requestContext) {
		this.requestInterceptors = requestInterceptors;
		this.workDir = workDir;
		this.requestContext = requestContext;
	}

	@SuppressWarnings("rawtypes")
	private Connector createHttpConnector(Dictionary dictionary) {
		Boolean httpEnabled = (Boolean) dictionary
				.get(JettyConstants.HTTP_ENABLED);
		if (httpEnabled != null && !httpEnabled.booleanValue()) {
			return null;
		}

		Integer httpPort = (Integer) dictionary.get(JettyConstants.HTTP_PORT);
		if (httpPort == null) {
			return null;
		}

		Boolean nioEnabled = (Boolean) dictionary.get(JettyConstants.HTTP_NIO);
		if (nioEnabled == null) {
			nioEnabled = getDefaultNIOEnablement();
		}

		Connector connector;
		if (nioEnabled.booleanValue()) {
			connector = new SelectChannelConnector();
		} else {
			connector = new SocketConnector();
		}

		connector.setPort(httpPort.intValue());

		String httpHost = (String) dictionary.get(JettyConstants.HTTP_HOST);
		if (httpHost != null) {
			connector.setHost(httpHost);
		}

		if (connector.getPort() == 0) {
			try {
				connector.open();
			} catch (IOException e) {
				// this would be unexpected since we're opening the next
				// available port
				e.printStackTrace();
			}
		}
		return connector;
	}

	private Context createHttpContext(Dictionary dictionary) {
		final String method = "createHttpContext(): ";
		Context httpContext = new Context();
		httpContext.setAttribute(INTERNAL_CONTEXT_CLASSLOADER, Thread
				.currentThread().getContextClassLoader());
		httpContext.setClassLoader(this.getClass().getClassLoader());

		String contextPathProperty = (String) dictionary
				.get(JettyConstants.CONTEXT_PATH);
		if (contextPathProperty == null) {
			contextPathProperty = "/"; //$NON-NLS-1$
		}
		httpContext.setContextPath(contextPathProperty);

		File contextWorkDir = new File(this.workDir, DIR_PREFIX
				+ dictionary.get(Constants.SERVICE_PID).hashCode());
		if (contextWorkDir.mkdir()) {
			LOG.debug(
					method + "create a directory {} as working directory for the HTTP server.",
					contextWorkDir.getAbsolutePath());
		} else {
			LOG.debug(
					method + "directory {} already exists which will be used as working directory for the HTTP server.",
					contextWorkDir.getAbsolutePath());
		}

		httpContext.setAttribute(CONTEXT_TEMPDIR, contextWorkDir);

		HashSessionManager sessionManager = new HashSessionManager();
		Integer sessionInactiveInterval = (Integer) dictionary
				.get(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL);
		if (sessionInactiveInterval != null) {
			sessionManager.setMaxInactiveInterval(sessionInactiveInterval.intValue());
		}

		httpContext.setSessionHandler(new SessionHandler(sessionManager));

		return httpContext;
	}

	@SuppressWarnings("rawtypes")
	private Connector createHttpsConnector(Dictionary dictionary) {
		Boolean httpsEnabled = (Boolean) dictionary
				.get(JettyConstants.HTTPS_ENABLED);
		if (httpsEnabled == null || !httpsEnabled.booleanValue()) {
			return null;
		}

		Integer httpsPort = (Integer) dictionary.get(JettyConstants.HTTPS_PORT);
		if (httpsPort == null) {
			return null;
		}

		SslSocketConnector sslConnector = new SslSocketConnector();
		sslConnector.setPort(httpsPort.intValue());

		String httpsHost = (String) dictionary.get(JettyConstants.HTTPS_HOST);
		if (httpsHost != null) {
			sslConnector.setHost(httpsHost);
		}

		String keyStore = (String) dictionary.get(JettyConstants.SSL_KEYSTORE);
		if (keyStore != null) {
			sslConnector.setKeystore(keyStore);
		}

		String password = (String) dictionary.get(JettyConstants.SSL_PASSWORD);
		if (password != null) {
			sslConnector.setPassword(password);
		}

		String keyPassword = (String) dictionary
				.get(JettyConstants.SSL_KEYPASSWORD);
		if (keyPassword != null) {
			sslConnector.setKeyPassword(keyPassword);
		}

		Object needClientAuth = dictionary
				.get(JettyConstants.SSL_NEEDCLIENTAUTH);
		if (needClientAuth != null) {
			if (needClientAuth instanceof String) {
				needClientAuth = Boolean.valueOf((String) needClientAuth);
			}
			sslConnector.setNeedClientAuth(((Boolean) needClientAuth).booleanValue());
		}

		Object wantClientAuth = dictionary.get(JettyConstants.SSL_WANTCLIENTAUTH);
		if (wantClientAuth != null) {
			if (wantClientAuth instanceof String) {
				wantClientAuth = Boolean.valueOf((String) wantClientAuth);
			}
			sslConnector.setWantClientAuth(((Boolean) wantClientAuth).booleanValue());
		}

		String protocol = (String) dictionary.get(JettyConstants.SSL_PROTOCOL);
		if (protocol != null) {
			sslConnector.setProtocol(protocol);
		}

		String keystoreType = (String) dictionary.get(JettyConstants.SSL_KEYSTORETYPE);
		if (keystoreType != null) {
			sslConnector.setKeystoreType(keystoreType);
		}

		if (sslConnector.getPort() == 0) {
			try {
				sslConnector.open();
			} catch (IOException e) {
				// this would be unexpected since we're opening the next
				// available port
				e.printStackTrace();
			}
		}
		return sslConnector;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void deleted(String pid) {
		final String method = "deleted(String) : ";
		Server server = (Server) this.servers.remove(pid);
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				String message = "Exception while removing a factory instance.";
				LOG.error(method + message, e);
				e.printStackTrace();
			}
			File contextWorkDir = new File(this.workDir, DIR_PREFIX
					+ pid.hashCode());
			deleteDirectory(contextWorkDir);
		}
	}

	private Boolean getDefaultNIOEnablement() {
		Properties systemProperties = System.getProperties();
		String javaVendor = systemProperties.getProperty("java.vendor", ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (javaVendor.equals("IBM Corporation")) //$NON-NLS-1$ 
		{
			String javaVersion = systemProperties.getProperty(
					"java.version", ""); //$NON-NLS-1$ //$NON-NLS-2$
			if (javaVersion.startsWith("1.4")) {
				return Boolean.FALSE;
			}
			// Note: no problems currently logged with 1.5
			if (javaVersion.equals("1.6.0")) //$NON-NLS-1$
			{
				String jclVersion = systemProperties.getProperty(
						"java.jcl.version", ""); //$NON-NLS-1$ //$NON-NLS-2$
				if (jclVersion.startsWith("2007")) {
					return Boolean.FALSE;
				}
				if (jclVersion.startsWith("2008")
						&& !jclVersion.startsWith("200811")
						&& !jclVersion.startsWith("200812")) {
					return Boolean.FALSE;
				}
			}
		}
		return Boolean.TRUE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getName() {
		return this.getClass().getName();
	}

	/**
	 * Stops all known servers and clears the server list
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public synchronized void shutdown() throws Exception {
		for (Iterator it = this.servers.values().iterator(); it.hasNext();) {
			Server server = (Server) it.next();
			server.stop();
		}
		this.servers.clear();
	}

	/**
	 * There are two modes for configuration the jetty server via the
	 * JettyCustomizer therefore see {@link JettyCustomizer}. And the other mode
	 * is via XML configuration, there the follow system properties must be set:
	 * 
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized void updated(String pid, Dictionary dictionary)
			throws ConfigurationException {
		deleted(pid);
		String serverName = dictionary.get("HTTP_SERVER_NAME").toString();
		Server server = new Server();

		JettyCustomizer customizer = createJettyCustomizer(dictionary);

		ServletHolder holder = new ServletHolder(
				new InternalHttpServiceServlet(requestContext,
						getRequestInterceptors()));
		holder.setInitOrder(0);
		holder.setInitParameter(Constants.SERVICE_VENDOR, "Eclipse.org"); //$NON-NLS-1$
		holder.setInitParameter(Constants.SERVICE_DESCRIPTION,
				"Equinox Jetty-based Http Service"); //$NON-NLS-1$
		holder.setInitParameter("http.service.name", serverName);

		Map props = (Map) dictionary.get("HTTP_SERVER_CUSTOM_SERVICE_PROPS");
		for (Object key : props.keySet()) {
			if (props.get(key) != null) {
				holder.setInitParameter(key.toString(), props.get(key).toString());
			}
		}

		if (dictionary.get("JETTY_XML_CONFIGURATION") != null) {
			try {
				File jettyConfiguration = JettyConfigurationUtils.getConfigurationFile(serverName);
				XmlConfiguration configuration = new XmlConfiguration(new FileInputStream(jettyConfiguration));
				configuration.configure(server);
				if(customizer != null){
					customizeServerConnectors(customizer, dictionary, server);
				}
			} catch (FileNotFoundException exp) {
				LOG.error("Configuration file for the HTTP Server server was not found !", exp);
			} catch (SAXException exp) {
				LOG.error("Error parsing the HTTP Server configuration file.", exp);
			} catch (IOException exp) {
				LOG.error("Error loading the HTTP Server configuration file.", exp);
			} catch (Exception exp) {
				LOG.error("Could not read the HTTP Server configuration", exp);
			}
		} else {
			Connector httpConnector = createHttpConnector(dictionary);
			if (null != customizer)
				httpConnector = (Connector) customizer.customizeHttpConnector(
						httpConnector, dictionary);
			if (httpConnector != null) {
				server.addConnector(httpConnector);
			}
			Connector httpsConnector = createHttpsConnector(dictionary);
			if (null != customizer)
				httpConnector = (Connector) customizer.customizeHttpConnector(
						httpsConnector, dictionary);
			if (httpsConnector != null) {
				server.addConnector(httpsConnector);
			}

			if (httpConnector != null) {
				int port = httpConnector.getLocalPort();
				if (port == -1) {
					port = httpConnector.getPort();
				}
				holder.setInitParameter(JettyConstants.HTTP_PORT,
						Integer.toString(port));
			}
			if (httpsConnector != null) {
				int port = httpsConnector.getLocalPort();
				if (port == -1) {
					port = httpsConnector.getPort();
				}
				holder.setInitParameter(JettyConstants.HTTPS_PORT,
						Integer.toString(port));
			}
			String otherInfo = (String) dictionary
					.get(JettyConstants.OTHER_INFO);
			if (otherInfo != null) {
				holder.setInitParameter(JettyConstants.OTHER_INFO, otherInfo);

			}

		}
		Context httpContext = createHttpContext(dictionary);
		if (null != customizer) {
			httpContext = (Context) customizer.customizeContext(httpContext,
					dictionary);
		}
		httpContext.addServlet(holder, "/*"); //$NON-NLS-1$
		server.addHandler(httpContext);
		try {
			server.start();
		} catch (Exception e) {
			throw new ConfigurationException(pid, e.getMessage(), e);
		}
		this.servers.put(pid, server);
	}

	private void customizeServerConnectors(JettyCustomizer customizer, Dictionary settings, Server server) {
		Connector[] connectors = server.getConnectors();
		if(connectors != null){
			for (Connector connector : connectors) {
				if (connector instanceof SslSocketConnector) {
					SslSocketConnector httpsConnector = (SslSocketConnector) connector;
					customizer.customizeHttpsConnector(httpsConnector, settings);
				}
				else {
					customizer.customizeHttpConnector(connector, settings);
				}
			}
		}
	}

	private JettyCustomizer createJettyCustomizer(Dictionary dictionary) {
		String customizerClass = (String) dictionary.get(JettyConstants.CUSTOMIZER_CLASS);
		if (null == customizerClass)
			return null;
		try {
			return (JettyCustomizer) Class.forName(customizerClass)
					.newInstance();
		} catch (Exception e) {
			LOG.error("Faild to create the jetty customizer!", e);
			return null;
		}
	}

	public List<HttpRequestInterceptor> getRequestInterceptors() {
		return requestInterceptors;
	}
}
