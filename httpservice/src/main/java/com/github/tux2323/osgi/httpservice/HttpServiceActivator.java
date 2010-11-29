/*******************************************************************************
 * Copyright (c) 2005, 2009 Cognos Incorporated, IBM Corporation and
 * others. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public
 * License v1.0 which accompanies this distribution, and is available
 * at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Cognos Incorporated - initial API and implementation
 * IBM Corporation - bug fixes and enhancements
 *******************************************************************************/

package com.github.tux2323.osgi.httpservice;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.startlevel.StartLevel;

public class HttpServiceActivator
{

  public static final String EXTERNAL_HTTP_SERVICE = "external.http.service";

  private static final String JETTY_WORK_DIR = "jettywork"; //$NON-NLS-1$

  private static final String DEFAULT_PID = "default"; //$NON-NLS-1$

  private static final String MANAGED_SERVICE_FACTORY_PID_INTERNAL =
      "com.telekom.idm.sam3.platform.httpextender.internal.config"; //$NON-NLS-1$

  private static final String MANAGED_SERVICE_FACTORY_PID_EXTERNAL =
      "com.telekom.idm.sam3.platform.httpextender.external.config"; //$NON-NLS-1$

  /**
   * OSGi Http Service suggest these properties for setting the
   * default ports
   */
  private static final String ORG_OSGI_SERVICE_HTTP_PORT = "org.osgi.service.http.port"; //$NON-NLS-1$

  private static final String ORG_OSGI_SERVICE_HTTP_PORT_INTERNAL =
      "org.osgi.service.internal.http.port"; //$NON-NLS-1$

  private static final String ORG_OSGI_SERVICE_HTTP_PORT_SECURE =
      "org.osgi.service.http.port.secure"; //$NON-NLS-1$

  // controls whether start() should automatically start an Http
  // Service based on BundleContext properties (default is true)
  // Note: only used if the bundle is explicitly started (e.g. not
  // "lazy" activated)
  private static final String AUTOSTART = "org.eclipse.equinox.http.jetty.autostart"; //$NON-NLS-1$

  // Jetty will use a basic stderr logger if no other logging
  // mechanism is provided.
  // This setting can be used to over-ride the stderr logger
  // threshold(and only this default logger)
  // Valid values are in increasing threshold: "debug", "info",
  // "warn", "error", and "off"
  // (default threshold is "warn")
  private static final String LOG_STDERR_THRESHOLD =
      "org.eclipse.equinox.http.jetty.log.stderr.threshold"; //$NON-NLS-1$

  private HttpServerManager internalHttpServerManager;

  private HttpServerManager externalhttpServerManager;

  private ServiceRegistration internalRegistration;

  private ServiceRegistration externalRegistration;

  private List<HttpRequestInterceptor> requestInterceptors;

  public HttpServiceActivator()
  {
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void start(BundleContext context) throws Exception
  {
    File jettyWorkDir = new File(context.getDataFile(""), JETTY_WORK_DIR); //$NON-NLS-1$ 
    jettyWorkDir.mkdir();
    setStdErrLogThreshold(context.getProperty(LOG_STDERR_THRESHOLD));
    this.internalHttpServerManager =
        new HttpServerManager(this.getRequestInterceptors(), jettyWorkDir);

    Dictionary internalSettings = createDefaultSettings(context);
    String internalPort = System.getProperty(ORG_OSGI_SERVICE_HTTP_PORT_INTERNAL, "9090");
    internalSettings.put(JettyConstants.HTTP_PORT, Integer.valueOf(internalPort));
    internalSettings.put(EXTERNAL_HTTP_SERVICE, "false");
    this.internalHttpServerManager.updated(DEFAULT_PID, internalSettings);
    Dictionary internalDictionary = new Hashtable();
    internalDictionary.put(Constants.SERVICE_PID, MANAGED_SERVICE_FACTORY_PID_INTERNAL);
    internalDictionary.put(EXTERNAL_HTTP_SERVICE, "true");
    this.internalRegistration =
        context.registerService(ManagedServiceFactory.class.getName(),
            this.internalHttpServerManager, internalDictionary);
    this.externalhttpServerManager =
        new HttpServerManager(this.getRequestInterceptors(), jettyWorkDir);
    Dictionary externalSettings = createDefaultSettings(context);
    externalSettings.put(EXTERNAL_HTTP_SERVICE, "true");
    this.externalhttpServerManager.updated(DEFAULT_PID, externalSettings);
    Dictionary externalDictionary = new Hashtable();
    externalDictionary.put(Constants.SERVICE_PID, MANAGED_SERVICE_FACTORY_PID_EXTERNAL);
    externalDictionary.put(EXTERNAL_HTTP_SERVICE, "true");
    this.externalRegistration =
        context.registerService(ManagedServiceFactory.class.getName(),
            this.externalhttpServerManager, externalDictionary);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void setStdErrLogThreshold(String property)
  {
    try
    {
      Class clazz = Class.forName("org.slf4j.Logger");
      Method method = clazz.getMethod("setThresholdLogger", new Class[] {String.class});
      method.invoke(null, new Object[] {property});
    }
    catch (Throwable t)
    {
      // ignore
    }
  }

  private boolean isBundleActivationPolicyUsed(BundleContext context)
  {
    ServiceReference reference = context.getServiceReference(StartLevel.class.getName());
    StartLevel sl = null;
    if (reference != null)
    {
      sl = (StartLevel) context.getService(reference);
    }
    if (sl != null)
    {
      try
      {
        Bundle bundle = context.getBundle();
        Method isBundleActivationPolicyUsed =
            StartLevel.class.getMethod("isBundleActivationPolicyUsed", new Class[] {Bundle.class}); //$NON-NLS-1$
        Boolean result = (Boolean) isBundleActivationPolicyUsed.invoke(sl, new Object[] {bundle});
        return result.booleanValue();
      }
      catch (Exception e)
      {
        // ignore
        // Bundle Activation Policy only available in StartLevel
        // Service 1.1
      }
      finally
      {
        context.ungetService(reference);
      }
    }
    return false;
  }

  public void stop(BundleContext context) throws Exception
  {
    this.internalRegistration.unregister();
    this.internalRegistration = null;
    this.externalRegistration.unregister();
    this.externalRegistration = null;
    this.externalhttpServerManager.shutdown();
    this.externalhttpServerManager = null;
    this.internalHttpServerManager.shutdown();
    this.internalHttpServerManager = null;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Dictionary createDefaultSettings(BundleContext context)
  {
    final String PROPERTY_PREFIX = "org.eclipse.equinox.http.jetty."; //$NON-NLS-1$
    Dictionary defaultSettings = new Hashtable();

    // PID
    defaultSettings.put(Constants.SERVICE_PID, DEFAULT_PID);

    // HTTP Enabled (default is true)
    String httpEnabledProperty = context.getProperty(PROPERTY_PREFIX + JettyConstants.HTTP_ENABLED);

    Boolean httpEnabled = null;
    if (httpEnabledProperty == null)
    {
      httpEnabled = Boolean.TRUE;
    }
    else
    {
      httpEnabled = Boolean.valueOf(httpEnabledProperty);
    }
    defaultSettings.put(JettyConstants.HTTP_ENABLED, httpEnabled);

    // HTTP Port
    String httpPortProperty = context.getProperty(PROPERTY_PREFIX + JettyConstants.HTTP_PORT);
    if (httpPortProperty == null)
    {
      httpPortProperty = context.getProperty(ORG_OSGI_SERVICE_HTTP_PORT);
    }

    int httpPort = 8080;
    if (httpPortProperty != null)
    {
      try
      {
        httpPort = Integer.parseInt(httpPortProperty);
      }
      catch (NumberFormatException e)
      {
        // (log this) ignore and use default
      }
    }
    defaultSettings.put(JettyConstants.HTTP_PORT, Integer.valueOf(httpPort));

    // HTTP Host (default is 0.0.0.0)
    String httpHost = context.getProperty(PROPERTY_PREFIX + JettyConstants.HTTP_HOST);
    if (httpHost != null)
    {
      defaultSettings.put(JettyConstants.HTTP_HOST, httpHost);
    }

    // HTTPS Enabled (default is false)
    Boolean httpsEnabled =
        new Boolean(context.getProperty(PROPERTY_PREFIX + JettyConstants.HTTPS_ENABLED));
    defaultSettings.put(JettyConstants.HTTPS_ENABLED, httpsEnabled);

    if (httpsEnabled.booleanValue())
    {
      // HTTPS Port
      String httpsPortProperty = context.getProperty(PROPERTY_PREFIX + JettyConstants.HTTPS_PORT);
      if (httpsPortProperty == null)
      {
        httpsPortProperty = context.getProperty(ORG_OSGI_SERVICE_HTTP_PORT_SECURE);
      }

      int httpsPort = 443;
      if (httpsPortProperty != null)
      {
        try
        {
          httpsPort = Integer.parseInt(httpsPortProperty);
        }
        catch (NumberFormatException e)
        {
          // (log this) ignore and use default
        }
      }
      defaultSettings.put(JettyConstants.HTTPS_PORT, new Integer(httpsPort));

      // HTTPS Host (default is 0.0.0.0)
      String httpsHost = context.getProperty(PROPERTY_PREFIX + JettyConstants.HTTPS_HOST);
      if (httpsHost != null)
      {
        defaultSettings.put(JettyConstants.HTTPS_HOST, httpsHost);
      }

      // SSL SETTINGS
      String keystore = context.getProperty(PROPERTY_PREFIX + JettyConstants.SSL_KEYSTORE);
      if (keystore != null)
      {
        defaultSettings.put(JettyConstants.SSL_KEYSTORE, keystore);
      }

      String password = context.getProperty(PROPERTY_PREFIX + JettyConstants.SSL_PASSWORD);
      if (password != null)
      {
        defaultSettings.put(JettyConstants.SSL_PASSWORD, password);
      }

      String keypassword = context.getProperty(PROPERTY_PREFIX + JettyConstants.SSL_KEYPASSWORD);
      if (keypassword != null)
      {
        defaultSettings.put(JettyConstants.SSL_KEYPASSWORD, keypassword);
      }

      String needclientauth =
          context.getProperty(PROPERTY_PREFIX + JettyConstants.SSL_NEEDCLIENTAUTH);
      if (needclientauth != null)
      {
        defaultSettings.put(JettyConstants.SSL_NEEDCLIENTAUTH, new Boolean(needclientauth));
      }

      String wantclientauth =
          context.getProperty(PROPERTY_PREFIX + JettyConstants.SSL_WANTCLIENTAUTH);
      if (wantclientauth != null)
      {
        defaultSettings.put(JettyConstants.SSL_WANTCLIENTAUTH, new Boolean(wantclientauth));
      }

      String protocol = context.getProperty(PROPERTY_PREFIX + JettyConstants.SSL_PROTOCOL);
      if (protocol != null)
      {
        defaultSettings.put(JettyConstants.SSL_PROTOCOL, protocol);
      }

      String algorithm = context.getProperty(PROPERTY_PREFIX + JettyConstants.SSL_ALGORITHM);
      if (algorithm != null)
      {
        defaultSettings.put(JettyConstants.SSL_ALGORITHM, algorithm);
      }

      String keystoretype = context.getProperty(PROPERTY_PREFIX + JettyConstants.SSL_KEYSTORETYPE);
      if (keystoretype != null)
      {
        defaultSettings.put(JettyConstants.SSL_KEYSTORETYPE, keystoretype);
      }
    }

    // Servlet Context Path
    String contextpath = context.getProperty(PROPERTY_PREFIX + JettyConstants.CONTEXT_PATH);
    if (contextpath != null)
    {
      defaultSettings.put(JettyConstants.CONTEXT_PATH, contextpath);
    }

    // Session Inactive Interval (timeout)
    String sessionInactiveInterval =
        context.getProperty(PROPERTY_PREFIX + JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL);
    if (sessionInactiveInterval != null)
    {
      try
      {
        defaultSettings.put(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL, new Integer(
            sessionInactiveInterval));
      }
      catch (NumberFormatException e)
      {
        // (log this) ignore
      }
    }

    // Other Info
    String otherInfo = context.getProperty(PROPERTY_PREFIX + JettyConstants.OTHER_INFO);
    if (otherInfo != null)
    {
      defaultSettings.put(JettyConstants.OTHER_INFO, otherInfo);
    }

    // customizer
    String customizerClass = context.getProperty(PROPERTY_PREFIX + JettyConstants.CUSTOMIZER_CLASS);
    if (customizerClass != null)
    {
      defaultSettings.put(JettyConstants.CUSTOMIZER_CLASS, customizerClass);
    }

    return defaultSettings;
  }

  public void setRequestInterceptors(List<HttpRequestInterceptor> requestInterceptors)
  {
    this.requestInterceptors = requestInterceptors;
  }

  public List<HttpRequestInterceptor> getRequestInterceptors()
  {
    return requestInterceptors;
  }
}
