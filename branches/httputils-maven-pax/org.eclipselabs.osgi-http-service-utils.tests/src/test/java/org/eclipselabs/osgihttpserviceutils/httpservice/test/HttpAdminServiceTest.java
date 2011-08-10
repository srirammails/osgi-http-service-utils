package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public class HttpAdminServiceTest {
	
	HttpServerInstance externalServerInstance;

	ServiceTracker httpAdminServiceTracker;

	HttpServerInstance internalServerInstance;

	HttpServerInstance jettyXmlServerInstance;

	HttpAdminService httpAdminService;
	
	BundleContext bundleContext;
	
	@Configuration()
	public Option[] config() {
		return options(
//				vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
				junitBundles(),
				equinox(),
				felix(),
				provision(
				mavenBundle("org.osgi", "org.osgi.compendium", "4.2.0"),
				mavenBundle("commons-io", "commons-io", "2.0.1"),
				mavenBundle("org.slf4j", "slf4j-api", "1.6.1"),
				mavenBundle("org.slf4j", "slf4j-simple", "1.6.1"),
				mavenBundle("org.mortbay.jetty", "servlet-api", "3.0.20100224"),
				mavenBundle("org.mortbay.jetty", "jetty", "6.1.26"),
				mavenBundle("org.mortbay.jetty", "jetty-util", "6.1.26"),
				mavenBundle("org.eclipse.equinox.http", "servlet", "1.0.0-v20070606"),
				mavenBundle("org.apache.felix", "org.apache.felix.scr","1.6.0"),
				wrappedBundle(mavenBundle("commons-httpclient", "commons-httpclient", "3.1")),
				wrappedBundle(mavenBundle("commons-codec", "commons-codec", "1.3")),
				scanDir("../org.eclipselabs.osgi-http-service-utils.api/target").filter("*.jar"),
				scanDir("../org.eclipselabs.osgi-http-service-utils.internal/target").filter("*.jar"))
		);
	}
	
	void setup() throws Exception {
		httpAdminServiceTracker = new ServiceTracker(bundleContext,
				HttpAdminService.class.getName(), null);
		httpAdminServiceTracker.waitForService(3000);
		httpAdminServiceTracker.open();
		httpAdminService = (HttpAdminService) httpAdminServiceTracker
				.getService();
		assertNotNull(httpAdminService);

		// Setup server port direct in Java code
		Map<String, String> serviceProperties = new HashMap<String, String>();
		serviceProperties.put("external.http.service", "false");
		internalServerInstance = httpAdminService.createHttpServer("internal")
				.serviceProperties(serviceProperties).port(9090).start();

		// Setup server port via system property
		System.setProperty("org.osgi.service.http.external.port", "8080");
		externalServerInstance = httpAdminService.createHttpServer("external")
				.serviceProperty("external.http.service", "true").start();

		// Setup server via jetty XML configuration
		InputStream resourceAsStream = getClass().getResourceAsStream(
				"jetty.xml");
		String tmpDir = System.getProperty("java.io.tmpdir");
		IOUtils.copy(resourceAsStream, new FileOutputStream(new File(tmpDir,
				"jetty-sample-jetty.xml")));
		System.setProperty("jetty.server.configuration.directory", tmpDir);
		jettyXmlServerInstance = httpAdminService.createHttpServer(
				"jetty-sample").start();
	}
	
	void tearDown() throws Exception {
		 internalServerInstance.shutdown();
		 externalServerInstance.shutdown();
		 jettyXmlServerInstance.shutdown();
		 httpAdminServiceTracker.close();
	}
	
	@Test
	public void testCustomServiceProperty_External(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		setup();
		
		String filterString = "(&(" + Constants.OBJECTCLASS + "="
				+ HttpService.class.getName()
				+ ")(external.http.service=true))";
		Filter filter = bundleContext.createFilter(filterString);
		ServiceTracker httpExternalServiceTracker = new ServiceTracker(
				bundleContext, filter, null);
		httpExternalServiceTracker.waitForService(3000);
		httpExternalServiceTracker.open();
		ServiceReference serviceReference = httpExternalServiceTracker
				.getServiceReference();
		assertEquals("external",
				serviceReference.getProperty("http.service.name"));
		
		tearDown();
	}

	@Test
	public void testCustomServiceProperty_Internal(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		setup();
		
		String filterString = "(&(" + Constants.OBJECTCLASS + "="
				+ HttpService.class.getName()
				+ ")(external.http.service=false))";
		Filter filter = bundleContext.createFilter(filterString);
		ServiceTracker httpExternalServiceTracker = new ServiceTracker(
				bundleContext, filter, null);
		httpExternalServiceTracker.waitForService(3000);
		httpExternalServiceTracker.open();
		ServiceReference serviceReference = httpExternalServiceTracker
				.getServiceReference();
		assertEquals("internal",
				serviceReference.getProperty("http.service.name"));
		
		tearDown();
	}

	@Test
	public void testInternalAndExternalHttpService(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		setup();
		
		HttpClient httpClient = new HttpClient();
		GetMethod internalRequest = new GetMethod("http://localhost:9090/hello");
		assertEquals(404, httpClient.executeMethod(internalRequest));

		GetMethod externalRequest = new GetMethod("http://localhost:8080/hello");
		assertEquals(404, httpClient.executeMethod(externalRequest));

		externalRequest = new GetMethod("http://localhost:8090/hello");
		assertEquals(404, httpClient.executeMethod(externalRequest));
		
		tearDown();
	}

	@Test
	public void testShutdownHttpServer(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		setup();

		HttpClient httpClient = new HttpClient();
		GetMethod internalRequest = new GetMethod("http://localhost:9090/hello");
		assertEquals(404, httpClient.executeMethod(internalRequest));
		internalServerInstance.shutdown();
		try {
			httpClient.executeMethod(internalRequest);
			fail("Connection refused expected!");
		} catch (ConnectException exp) {
			assertTrue(true);
		}
		
		tearDown();
	}

}
