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

public class HttpServerManager implements ManagedServiceFactory
{

  private static Logger logger = LoggerFactory
      .getLogger(HttpServerManager.class);

  private static final String CONTEXT_TEMPDIR = "javax.servlet.context.tempdir"; //$NON-NLS-1$

  private static final String DIR_PREFIX = "pid_"; //$NON-NLS-1$

  private static final String INTERNAL_CONTEXT_CLASSLOADER = "org.eclipse.equinox.http.jetty.internal.ContextClassLoader"; //$NON-NLS-1$

  @SuppressWarnings("rawtypes")
  private final Map servers = new HashMap();

  private final File workDir;

  private final List<HttpRequestInterceptor> requestInterceptors;

  /**
   * Creates a new {@link HttpServerManager}
   * 
   * @param requestContextServiceImpl a
   *        {@link RequestContextServiceImpl}
   * @param requestInterceptors a list of
   *        {@link HttpRequestInterceptor}s
   * @param workDir the working directory
   */
  public HttpServerManager(List<HttpRequestInterceptor> requestInterceptors,
      File workDir)
  {
    this.requestInterceptors = requestInterceptors;
    this.workDir = workDir;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized void deleted(String pid)
  {
    final String method = "deleted(String) : ";
    Server server = (Server) this.servers.remove(pid);
    if (server != null)
    {
      try
      {
        server.stop();
      }
      catch (Exception e)
      {
        String message = "Exception while removing a factory instance.";
        logger.error(method + message, e);
        e.printStackTrace();
      }
      File contextWorkDir = new File(this.workDir, DIR_PREFIX + pid.hashCode());
      deleteDirectory(contextWorkDir);
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getName()
  {
    return this.getClass().getName();
  }

  /**
   *  There are two modes for configuration the jetty server via the JettyCustomizer therefore see {@link JettyCustomizer}.
   *  And the other mode is via XML configuration, there the follow system properties must be set: 
   *  
   * -Djetty.xml.configuration=true
   * -Djetty.xml.configuration.external="D:\\tmp\\jetty.xml"
   * -Djetty.xml.configuration.internal="D:\\tmp\\jetty-internal.xml"
   * 
   * {@inheritDoc}
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public synchronized void updated(String pid, Dictionary dictionary)
      throws ConfigurationException
  {
    deleted(pid);
    Server server = new Server();

    ServletHolder holder = new ServletHolder(new InternalHttpServiceServlet(
        requestInterceptors));
    holder.setInitOrder(0);
    holder.setInitParameter(Constants.SERVICE_VENDOR, "Eclipse.org"); //$NON-NLS-1$
    holder.setInitParameter(Constants.SERVICE_DESCRIPTION,
        "Equinox Jetty-based Http Service"); //$NON-NLS-1$
    holder.setInitParameter(HttpServiceActivator.EXTERNAL_HTTP_SERVICE,
        (String) dictionary.get(HttpServiceActivator.EXTERNAL_HTTP_SERVICE));
    
    JettyCustomizer customizer = createJettyCustomizer(dictionary);

    if (isXmlConfiguration())
    {
      try
      {
        if (isExternalHttpService(dictionary))
        {
          String externalJettyConfiguration = System.getProperty("jetty.xml.configuration.external");
          XmlConfiguration configuration = new XmlConfiguration(
              new FileInputStream(externalJettyConfiguration));
          configuration.configure(server);
        }
        else
        {
          String internalJettyConfiguration = System.getProperty("jetty.xml.configuration.internal");
          XmlConfiguration configuration = new XmlConfiguration(
              new FileInputStream(internalJettyConfiguration));
          configuration.configure(server);
        }
      }
      catch (FileNotFoundException exp)
      {
        logger.error("Could not start the HTTP Server", exp);
      }
      catch (SAXException exp)
      {
        logger.error("Could not start the HTTP Server", exp);
      }
      catch (IOException exp)
      {
        logger.error("Could not start the HTTP Server", exp);
      }
      catch (Exception exp)
      {
        logger.error("Could not start the HTTP Server", exp);
      }
    }
    else
    {
      Connector httpConnector = createHttpConnector(dictionary);
      if (null != customizer)
      {
        httpConnector = (Connector) customizer.customizeHttpConnector(
            httpConnector, dictionary);
        httpConnector.setHeaderBufferSize(16192);
      }

      if (httpConnector != null)
      {
        server.addConnector(httpConnector);
      }

      Connector httpsConnector = createHttpsConnector(dictionary);
      if (null != customizer)
      {
        httpsConnector = (Connector) customizer.customizeHttpsConnector(
            httpsConnector, dictionary);
      }
      if (httpsConnector != null)
      {
        server.addConnector(httpsConnector);
      }

      if (httpConnector != null)
      {
        int port = httpConnector.getLocalPort();
        if (port == -1)
        {
          port = httpConnector.getPort();
        }
        holder.setInitParameter(JettyConstants.HTTP_PORT,
            Integer.toString(port));
      }
      if (httpsConnector != null)
      {
        int port = httpsConnector.getLocalPort();
        if (port == -1)
        {
          port = httpsConnector.getPort();
        }
        holder.setInitParameter(JettyConstants.HTTPS_PORT,
            Integer.toString(port));
      }
      String otherInfo = (String) dictionary.get(JettyConstants.OTHER_INFO);
      if (otherInfo != null)
      {
        holder.setInitParameter(JettyConstants.OTHER_INFO, otherInfo);

      }

    }
    Context httpContext = createHttpContext(dictionary);
    if (null != customizer)
    {
      httpContext = (Context) customizer.customizeContext(httpContext,
          dictionary);
    }

    httpContext.addServlet(holder, "/*"); //$NON-NLS-1$
    server.addHandler(httpContext);

    try
    {
      server.start();
    }
    catch (Exception e)
    {
      throw new ConfigurationException(pid, e.getMessage(), e);
    }
    this.servers.put(pid, server);
  }

  private boolean isXmlConfiguration()
  {
    String jettyXmlConfiguration = System.getProperty("jetty.xml.configuration");
    return jettyXmlConfiguration != null && jettyXmlConfiguration.equals("true");
  }

  private boolean isExternalHttpService(Dictionary dictionary)
  {
    return dictionary.get("external.http.service").equals("true");
  }

  /**
   * Stops all known servers and clears the server list
   * 
   * @throws Exception
   */
  @SuppressWarnings("rawtypes")
  public synchronized void shutdown() throws Exception
  {
    for (Iterator it = this.servers.values().iterator(); it.hasNext();)
    {
      Server server = (Server) it.next();
      server.stop();
    }
    this.servers.clear();
  }

  @SuppressWarnings("rawtypes")
  private Connector createHttpConnector(Dictionary dictionary)
  {
    Boolean httpEnabled = (Boolean) dictionary.get(JettyConstants.HTTP_ENABLED);
    if (httpEnabled != null && !httpEnabled.booleanValue())
    {
      return null;
    }

    Integer httpPort = (Integer) dictionary.get(JettyConstants.HTTP_PORT);
    if (httpPort == null)
    {
      return null;
    }

    Boolean nioEnabled = (Boolean) dictionary.get(JettyConstants.HTTP_NIO);
    if (nioEnabled == null)
    {
      nioEnabled = getDefaultNIOEnablement();
    }

    Connector connector;
    if (nioEnabled.booleanValue())
    {
      connector = new SelectChannelConnector();
    }
    else
    {
      connector = new SocketConnector();
    }

    connector.setPort(httpPort.intValue());

    String httpHost = (String) dictionary.get(JettyConstants.HTTP_HOST);
    if (httpHost != null)
    {
      connector.setHost(httpHost);
    }

    if (connector.getPort() == 0)
    {
      try
      {
        connector.open();
      }
      catch (IOException e)
      {
        // this would be unexpected since we're opening the next
        // available port
        e.printStackTrace();
      }
    }
    return connector;
  }

  private Boolean getDefaultNIOEnablement()
  {
    Properties systemProperties = System.getProperties();
    String javaVendor = systemProperties.getProperty("java.vendor", ""); //$NON-NLS-1$ //$NON-NLS-2$
    if (javaVendor.equals("IBM Corporation")) //$NON-NLS-1$ 
    {
      String javaVersion = systemProperties.getProperty("java.version", ""); //$NON-NLS-1$ //$NON-NLS-2$
      if (javaVersion.startsWith("1.4"))
      {
        return Boolean.FALSE;
      }
      // Note: no problems currently logged with 1.5
      if (javaVersion.equals("1.6.0")) //$NON-NLS-1$
      {
        String jclVersion = systemProperties
            .getProperty("java.jcl.version", ""); //$NON-NLS-1$ //$NON-NLS-2$
        if (jclVersion.startsWith("2007"))
        {
          return Boolean.FALSE;
        }
        if (jclVersion.startsWith("2008") && !jclVersion.startsWith("200811")
            && !jclVersion.startsWith("200812"))
        {
          return Boolean.FALSE;
        }
      }
    }
    return Boolean.TRUE;
  }

  @SuppressWarnings("rawtypes")
  private Connector createHttpsConnector(Dictionary dictionary)
  {
    Boolean httpsEnabled = (Boolean) dictionary
        .get(JettyConstants.HTTPS_ENABLED);
    if (httpsEnabled == null || !httpsEnabled.booleanValue())
    {
      return null;
    }

    Integer httpsPort = (Integer) dictionary.get(JettyConstants.HTTPS_PORT);
    if (httpsPort == null)
    {
      return null;
    }

    SslSocketConnector sslConnector = new SslSocketConnector();
    sslConnector.setPort(httpsPort.intValue());

    String httpsHost = (String) dictionary.get(JettyConstants.HTTPS_HOST);
    if (httpsHost != null)
    {
      sslConnector.setHost(httpsHost);
    }

    String keyStore = (String) dictionary.get(JettyConstants.SSL_KEYSTORE);
    if (keyStore != null)
    {
      sslConnector.setKeystore(keyStore);
    }

    String password = (String) dictionary.get(JettyConstants.SSL_PASSWORD);
    if (password != null)
    {
      sslConnector.setPassword(password);
    }

    String keyPassword = (String) dictionary
        .get(JettyConstants.SSL_KEYPASSWORD);
    if (keyPassword != null)
    {
      sslConnector.setKeyPassword(keyPassword);
    }

    Object needClientAuth = dictionary.get(JettyConstants.SSL_NEEDCLIENTAUTH);
    if (needClientAuth != null)
    {
      if (needClientAuth instanceof String)
      {
        needClientAuth = Boolean.valueOf((String) needClientAuth);
      }

      sslConnector.setNeedClientAuth(((Boolean) needClientAuth).booleanValue());
    }

    Object wantClientAuth = dictionary.get(JettyConstants.SSL_WANTCLIENTAUTH);
    if (wantClientAuth != null)
    {
      if (wantClientAuth instanceof String)
      {
        wantClientAuth = Boolean.valueOf((String) wantClientAuth);
      }

      sslConnector.setWantClientAuth(((Boolean) wantClientAuth).booleanValue());
    }

    String protocol = (String) dictionary.get(JettyConstants.SSL_PROTOCOL);
    if (protocol != null)
    {
      sslConnector.setProtocol(protocol);
    }

    String keystoreType = (String) dictionary
        .get(JettyConstants.SSL_KEYSTORETYPE);
    if (keystoreType != null)
    {
      sslConnector.setKeystoreType(keystoreType);
    }

    if (sslConnector.getPort() == 0)
    {
      try
      {
        sslConnector.open();
      }
      catch (IOException e)
      {
        // this would be unexpected since we're opening the next
        // available port
        e.printStackTrace();
      }
    }
    return sslConnector;
  }

  private Context createHttpContext(Dictionary dictionary)
  {
    Context httpContext = new Context();
    httpContext.setAttribute(INTERNAL_CONTEXT_CLASSLOADER, Thread
        .currentThread().getContextClassLoader());
    httpContext.setClassLoader(this.getClass().getClassLoader());

    String contextPathProperty = (String) dictionary
        .get(JettyConstants.CONTEXT_PATH);
    if (contextPathProperty == null)
    {
      contextPathProperty = "/"; //$NON-NLS-1$
    }
    httpContext.setContextPath(contextPathProperty);

    File contextWorkDir = new File(this.workDir, DIR_PREFIX
        + dictionary.get(Constants.SERVICE_PID).hashCode());
    contextWorkDir.mkdir();
    httpContext.setAttribute(CONTEXT_TEMPDIR, contextWorkDir);

    HashSessionManager sessionManager = new HashSessionManager();
    Integer sessionInactiveInterval = (Integer) dictionary
        .get(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL);
    if (sessionInactiveInterval != null)
    {
      sessionManager.setMaxInactiveInterval(sessionInactiveInterval.intValue());
    }

    httpContext.setSessionHandler(new SessionHandler(sessionManager));

    return httpContext;
  }

  @SuppressWarnings("rawtypes")
  private JettyCustomizer createJettyCustomizer(Dictionary dictionary)
  {
    final String method = "createJettyCustomizer(Dictionary)";
    String customizerClass = (String) dictionary
        .get(JettyConstants.CUSTOMIZER_CLASS);
    if (null == customizerClass)
    {
      return null;
    }

    try
    {
      return (JettyCustomizer) Class.forName(customizerClass).newInstance();
    }
    catch (Exception e)
    {
      String message = "Exception while creating JettyCustomizer";
      logger.error(method + message, e);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Internal {@link Servlet} implementation
   */
  public static class InternalHttpServiceServlet implements Servlet
  {
    private static final long serialVersionUID = 7477982882399972088L;

    private final Servlet httpServiceServlet = new HttpServiceServlet();

    private ClassLoader contextLoader;

    private final List<HttpRequestInterceptor> requestInterceptors;

    /**
     * Creates a new {@link InternalHttpServiceServlet}
     * @param requestContextServiceImpl the
     *        {@link RequestContextServiceImpl}
     * @param requestInterceptors a list of
     *        {@link HttpRequestInterceptor}s
     */
    public InternalHttpServiceServlet(
        List<HttpRequestInterceptor> requestInterceptors)
    {
      this.requestInterceptors = requestInterceptors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig config) throws ServletException
    {
      ServletContext context = config.getServletContext();
      this.contextLoader = (ClassLoader) context
          .getAttribute(INTERNAL_CONTEXT_CLASSLOADER);

      Thread thread = Thread.currentThread();
      ClassLoader current = thread.getContextClassLoader();
      thread.setContextClassLoader(this.contextLoader);
      try
      {
        this.httpServiceServlet.init(config);
      }
      finally
      {
        thread.setContextClassLoader(current);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy()
    {
      Thread thread = Thread.currentThread();
      ClassLoader current = thread.getContextClassLoader();
      thread.setContextClassLoader(this.contextLoader);
      try
      {
        this.httpServiceServlet.destroy();
      }
      finally
      {
        thread.setContextClassLoader(current);
      }
      this.contextLoader = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException
    {
      Thread thread = Thread.currentThread();
      ClassLoader current = thread.getContextClassLoader();
      thread.setContextClassLoader(this.contextLoader);
      try
      {
        for (HttpRequestInterceptor interceptor : this.requestInterceptors)
        {
          try
          {
            boolean isIntercepted = interceptor.beforeRequest(req, res);
            if (isIntercepted)
            {
              return;
            }
          }
          catch (Exception exp)
          {
            logger.warn("A exception was thrown on invoking a request interceptor", exp);
          }
        }
      }
      catch (Exception exp)
      {
        logger.warn("A exception was thrown on invoking a request interceptor", exp);
      }
      try
      {
        this.httpServiceServlet.service(req, res);
      }
      finally
      {
        thread.setContextClassLoader(current);
      }
      try
      {
        for (HttpRequestInterceptor interceptor : this.requestInterceptors)
        {
          try
          {
            boolean isIntercepted = interceptor.afterRequest(req, res);
            if (isIntercepted)
            {
              return;
            }
          }
          catch (Exception exp)
          {
            logger.warn("A exception was thrown on invoking a interceptor after request.", exp);
          }
        }
      }
      catch (Exception exp)
      {
        logger.warn("A exception was thrown on invoking a interceptor after request.", exp);
      }
    }

    public ServletConfig getServletConfig()
    {
      return this.httpServiceServlet.getServletConfig();
    }

    public String getServletInfo()
    {
      return this.httpServiceServlet.getServletInfo();
    }
  }

  // deleteDirectory is a convenience method to recursively delete a
  // directory
  private static boolean deleteDirectory(File directory)
  {
    if (directory.exists() && directory.isDirectory())
    {
      File[] files = directory.listFiles();
      for (int i = 0; i < files.length; i++)
      {
        if (files[i].isDirectory())
        {
          deleteDirectory(files[i]);
        }
        else
        {
          files[i].delete();
        }
      }
    }
    return directory.delete();
  }
}
