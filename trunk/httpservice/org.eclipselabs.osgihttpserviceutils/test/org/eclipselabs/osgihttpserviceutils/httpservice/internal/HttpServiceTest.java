package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpRequestInterceptor;
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
	
	static final String JETTY_SERVER_CONFIGURATION_DIRECTORY = "jetty.server.configuration.directory";
	
	static final String TMP_DIR = System.getProperty("java.io.tmpdir");

	@Rule
	public JUnitRuleMockery mockery = new JUnitRuleMockery(){{
		setImposteriser(ClassImposteriser.INSTANCE);
	}};
	
	@Mock BundleContext bundleContext;
	
	@Mock ServiceRegistration serviceRegistration;
	
	@Mock HttpServerManager httpServerManager;
		
	Dictionary<Object,Object> settings;
	
	HttpServer httpServer;
	
	HttpService httpService;
	
	String httpEnablePropertyValue;

	@Before public void setUp() throws Exception {
		httpService = createHttService();
		
		httpServer = httpService
			.createHttpServer("defaultHttpServer")
			.serviceProperties(new HashMap<Object, Object>())
			.port(8080);
		
		settings = new Properties();
		settings.put(Constants.SERVICE_PID, "default");
		settings.put(JettyConstants.HTTP_ENABLED, true);
		settings.put(JettyConstants.HTTP_HOST, "0.0.0.0");
		settings.put(JettyConstants.HTTPS_ENABLED, false);
		settings.put(JettyConstants.CONTEXT_PATH, "/");
		settings.put(JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL, 6000);
		settings.put(JettyConstants.OTHER_INFO, "Other Jetty Infos");
		settings.put("HTTP_SERVER_CUSTOM_SERVICE_PROPS", httpServer.getServiceProperties());
		
		mockery.checking(new Expectations(){{
			oneOf(bundleContext).getProperty("org.eclipse.equinox.http.jetty.log.stderr.threshold");
			will(returnValue(null));
		}});
		httpService.activate(bundleContext);
		
		httpEnablePropertyValue = "true";
	}
	
	@After public void tearDown(){
		resetJettyConfigurationArea();
		System.clearProperty("org.osgi.service.http." + httpServer.getSymbolicName() + ".port");
	}
	
	@Test public void startServer() throws Exception {
		mockBundleContextProperties();
		settings.put(JettyConstants.HTTP_PORT, httpServer.getPort());
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		assertThat(httpServer.start(), isIn(httpService.getHttpServerInstances()));
	}
	
	@Test public void startServer_HttpEnablePropertyNotSet() throws Exception {
		httpEnablePropertyValue = null;
		mockBundleContextProperties();
		settings.put(JettyConstants.HTTP_PORT, httpServer.getPort());
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		assertThat(httpServer.start(), isIn(httpService.getHttpServerInstances()));
	}
	
	@Test public void startServer_WithJettyConfiguration() throws Exception {
		mockBundleContextProperties();
		setupJettyConfigurationArea();
		settings.put(JettyConstants.JETTY_XML_CONFIGURATION, true);
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		assertThat(httpServer.port(0).start(), isIn(httpService.getHttpServerInstances()));
	}
	
	@Test public void startServer_WithSystemPropertyConfiguration() throws Exception {
		resetJettyConfigurationArea();
		System.setProperty("org.osgi.service.http." + httpServer.getSymbolicName() + ".port", "9090");
		mockBundleContextProperties();
		settings.put(JettyConstants.HTTP_PORT, 9090);
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		assertThat(httpServer.port(0).start(), isIn(httpService.getHttpServerInstances()));
	}
	
	@Test(expected = HttpServiceInternalException.class) 
	public void startServer_NoConfiguration() {
		resetJettyConfigurationArea();
		mockBundleContextProperties();
		httpServer.port(0).start();
	}
	
	@Test(expected = HttpServiceInternalException.class)
	public void startServer_HttpServiceInternalException() throws Exception {
		mockBundleContextProperties();
		mockery.checking(new Expectations(){{
			oneOf(httpServerManager).updated(with(HttpService.DEFAULT_PID), with(any(Dictionary.class)));
			will(throwException(new ConfigurationException("","")));
		}});
		httpServer.start();
	}
	
	@Test public void shutdown() throws Exception {
		mockBundleContextProperties();
		settings.put(JettyConstants.HTTP_PORT, httpServer.getPort());
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		mockery.checking(new Expectations(){{
			oneOf(serviceRegistration).unregister();
			oneOf(httpServerManager).shutdown();
		}});
		httpServer.start().shutdown();
		assertThat(httpService.getHttpServerInstances(), hasSize(0));
	}
	
	@Test(expected = HttpServiceInternalException.class)
	public void shutdown_InternalError() throws Exception {
		mockBundleContextProperties();
		settings.put(JettyConstants.HTTP_PORT, httpServer.getPort());
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		mockery.checking(new Expectations(){{
			oneOf(serviceRegistration).unregister();
			oneOf(httpServerManager).shutdown();
			will(throwException(new Exception()));
		}});
		httpService.startServer(httpServer).shutdown();
	}
	
	@Test(expected = UnsupportedOperationException.class) 
	public void reloadConfiguration() throws Exception {
		mockBundleContextProperties();
		settings.put(JettyConstants.HTTP_PORT, httpServer.getPort());
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		httpService.startServer(httpServer).reloadConfiguration();
	}
	
	@Test public void addRequestInterceptors() throws Exception {
		HttpRequestInterceptor requestInterceptor = mockery.mock(HttpRequestInterceptor.class);
		httpService.addRequestInterceptors(requestInterceptor);
		assertThat(httpService.getRequestInterceptors(), contains(requestInterceptor));
	}
	
	@Test public void deactivate() throws Exception {
		mockBundleContextProperties();
		settings.put(JettyConstants.HTTP_PORT, httpServer.getPort());
		settings.put(JettyConstants.HTTP_SERVER_NAME, httpServer.getSymbolicName());
		mockHttpServerManagerUpdate();
		mockBundleContextRegisterService();
		mockery.checking(new Expectations(){{
			oneOf(serviceRegistration).unregister();
			oneOf(httpServerManager).shutdown();
		}});
		httpServer.start();
		httpService.deactivate(bundleContext);
		assertThat(httpService.getHttpServerInstances(), hasSize(0));
	}
	
	@Test public void createHttpServer() throws Exception {
		assertThat(httpServer, is(httpService.createHttpServer(httpServer.getSymbolicName())));
	}
	
	@Test public void getRequestContext() throws Exception {
		assertThat(httpService.getRequestContext(), not(nullValue()));
	}
	
	@Test public void removeRequestInterceptors() throws Exception {
		HttpRequestInterceptor requestInterceptor = mockery.mock(HttpRequestInterceptor.class);
		httpService.addRequestInterceptors(requestInterceptor);
		httpService.removeRequestInterceptors(requestInterceptor);
		assertThat(httpService.getRequestInterceptors(), not(contains(requestInterceptor)));
	}
	
	@Test public void createHttpServerManager_RequestInterceptors() throws Exception {
		httpService = new HttpService();
		HttpRequestInterceptor requestInterceptor = mockery.mock(HttpRequestInterceptor.class);
		httpService.addRequestInterceptors(requestInterceptor);
		HttpServerManager httpServerManager = httpService.createHttpServerManager();
		assertThat(httpServerManager.getRequestInterceptors(), contains(requestInterceptor));
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
			oneOf(bundleContext).getProperty(HttpService.PROPERTY_PREFIX + JettyConstants.HTTP_ENABLED);
			will(returnValue(httpEnablePropertyValue));
			
			oneOf(bundleContext).getProperty(HttpService.PROPERTY_PREFIX + JettyConstants.HTTP_HOST);
			will(returnValue("0.0.0.0"));
			
			oneOf(bundleContext).getProperty(HttpService.PROPERTY_PREFIX + JettyConstants.HTTPS_ENABLED);
			will(returnValue("false"));
			
			oneOf(bundleContext).getProperty(HttpService.PROPERTY_PREFIX + JettyConstants.CONTEXT_PATH);
			will(returnValue("/"));
			
			oneOf(bundleContext).getProperty(HttpService.PROPERTY_PREFIX + JettyConstants.CONTEXT_SESSIONINACTIVEINTERVAL);
			will(returnValue("6000"));
			
			oneOf(bundleContext).getProperty(HttpService.PROPERTY_PREFIX + JettyConstants.OTHER_INFO);
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
