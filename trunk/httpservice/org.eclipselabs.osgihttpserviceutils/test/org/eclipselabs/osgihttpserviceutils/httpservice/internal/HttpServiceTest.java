package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServer;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class HttpServiceTest {
	
	private static final String PREFIX_HTTP_JETTY_PROPERTIES = "org.eclipse.equinox.http.jetty.";

	static final String JETTY_SERVER_CONFIGURATION_DIRECTORY = "jetty.server.configuration.directory";
	
	static final String TMP_DIR = System.getProperty("java.io.tmpdir");

	@Rule
	public JUnitRuleMockery mockery = new JUnitRuleMockery(){{
		setImposteriser(ClassImposteriser.INSTANCE);
	}};
	
	HttpServer httpServer;
	
	@Mock BundleContext bundleContext;
	
	@Mock ServiceRegistration serviceRegistration;
	
	@Mock HttpServerManager httpServerManager;
	
	int httpPort = 8080;
	
	Map<Object, Object> serviceProperties;
	
	Dictionary settings;
	
	HttpService httpService;

	@Before public void setUp() throws Exception {
		serviceProperties = new HashMap<Object, Object>();
		settings = new Properties();
		settings.put(Constants.SERVICE_PID, "default");
		settings.put(JettyConstants.HTTP_ENABLED, true);
		settings.put(JettyConstants.HTTP_HOST, "0.0.0.0");
		settings.put(JettyConstants.HTTPS_ENABLED, false);
		settings.put(JettyConstants.CONTEXT_PATH, "/");
		settings.put(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL, 6000);
		settings.put(JettyConstants.OTHER_INFO, "Other Jetty Infos");
		settings.put("HTTP_SERVER_CUSTOM_SERVICE_PROPS", serviceProperties);
		httpService = createHttService();
		mockery.checking(new Expectations(){{
			oneOf(bundleContext).getProperty("org.eclipse.equinox.http.jetty.log.stderr.threshold");
			will(returnValue(null));
		}});
		httpService.activate(bundleContext);
	}
	
	@After public void tearDown(){
		resetJettyConfigurationArea();
	}
	
	@Test public void startServer_withConfiguredPort() throws Exception {
		mockBundleContextProperties();
		httpServer = httpService.createHttpServer("defaultHttpServer").serviceProperties(serviceProperties).port(httpPort);
		settings.put(JettyConstants.HTTP_PORT, httpPort);
		settings.put("HTTP_SERVER_NAME", "defaultHttpServer");
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		assertThat(httpService.startServer(httpServer), isIn(httpService.getHttpServerInstances()));
	}
	
	@Test public void startServer_WithNoConfiguredPort() throws Exception {
		mockBundleContextProperties();
		httpServer = httpService.createHttpServer("defaultHttpServer").serviceProperties(serviceProperties).port(0);
		setupJettyConfigurationArea();
		settings.put(JettyConstants.JETTY_XML_CONFIGURATION, true);
		settings.put("HTTP_SERVER_NAME", "defaultHttpServer");
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		assertThat(httpService.startServer(httpServer), isIn(httpService.getHttpServerInstances()));
	}
	
	@Test(expected = HttpServiceInternalException.class)
	public void startServer_HttpServiceInternalException() throws Exception {
		mockBundleContextProperties();
		httpServer = httpService.createHttpServer("defaultHttpServer").serviceProperties(serviceProperties).port(8080);
		mockery.checking(new Expectations(){{
			oneOf(httpServerManager).updated(with(HttpService.DEFAULT_PID), with(any(Dictionary.class)));
			will(throwException(new ConfigurationException("","")));
		}});
		httpService.startServer(httpServer);
	}
	
	@Test public void shutdown() throws Exception {
		mockBundleContextProperties();
		httpServer = httpService.createHttpServer("defaultHttpServer").serviceProperties(serviceProperties).port(8080);
		settings.put(JettyConstants.HTTP_PORT, httpPort);
		settings.put("HTTP_SERVER_NAME", "defaultHttpServer");
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		mockery.checking(new Expectations(){{
			oneOf(serviceRegistration).unregister();
			oneOf(httpServerManager).shutdown();
		}});
		httpService.startServer(httpServer).shutdown();
		assertThat(httpService.getHttpServerInstances(), hasSize(0));
	}
	
	HttpService createHttService(){
		return new HttpService(){
			protected HttpServerManager createHttpServerManager() {
				return httpServerManager;
			};
		};
	}
	
	void resetJettyConfigurationArea() {
		System.clearProperty(JETTY_SERVER_CONFIGURATION_DIRECTORY);
	}
	
	void setupJettyConfigurationArea() throws Exception {
		System.setProperty(JETTY_SERVER_CONFIGURATION_DIRECTORY, TMP_DIR);
		File testJettyConfiguration = new File(getClass().getResource("jetty.xml").getFile());
		File jettyConfiguration = new File(TMP_DIR, "defaultHttpServer-jetty.xml");
		IOUtils.copy(new FileInputStream(testJettyConfiguration), new FileOutputStream(jettyConfiguration));
	}
	
	void mockBundleContextProperties() {
		mockery.checking(new Expectations(){{
			oneOf(bundleContext).getProperty(PREFIX_HTTP_JETTY_PROPERTIES + JettyConstants.HTTP_ENABLED);
			will(returnValue("true"));
			
			oneOf(bundleContext).getProperty(PREFIX_HTTP_JETTY_PROPERTIES + JettyConstants.HTTP_HOST);
			will(returnValue("0.0.0.0"));
			
			oneOf(bundleContext).getProperty(PREFIX_HTTP_JETTY_PROPERTIES + JettyConstants.HTTPS_ENABLED);
			will(returnValue("false"));
			
			oneOf(bundleContext).getProperty(PREFIX_HTTP_JETTY_PROPERTIES + JettyConstants.CONTEXT_PATH);
			will(returnValue("/"));
			
			oneOf(bundleContext).getProperty(PREFIX_HTTP_JETTY_PROPERTIES + JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL);
			will(returnValue("6000"));
			
			oneOf(bundleContext).getProperty(PREFIX_HTTP_JETTY_PROPERTIES + JettyConstants.OTHER_INFO);
			will(returnValue("Other Jetty Infos"));
		}});
	}
	
	void mockBundleContextRegisterService() {
		mockery.checking(new Expectations(){{
			oneOf(bundleContext).registerService(
					with(ManagedServiceFactory.class.getName()), 
					with(httpServerManager), 
					with(any(Dictionary.class)));
			will(returnValue(serviceRegistration));
		}});
	}

	void mockHttpServerManagerUpdate() throws ConfigurationException {
		mockery.checking(new Expectations(){{
			oneOf(httpServerManager).updated(with(HttpService.DEFAULT_PID), with(settings));
		}});
	}

}
