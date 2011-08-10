package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpRequestInterceptor;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.eclipselabs.osgihttpserviceutils.httpservice.RequestService;
import org.eclipselabs.osgihttpserviceutils.httpservice.test.utils.Interceptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(JUnit4TestRunner.class)
public class HttpRequestInterceptorTest {
	
	@Configuration()
	public Option[] config() {
		return options(
//				uncomment for remote debugging the test
//				vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
				junitBundles(),
				equinox(),
//				felix(),
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
	
	
	ServiceTracker httpAdminServiceTracker;

	HttpServerInstance httpServerInstance;

	ServiceTracker httpServiceTracker;

	ServiceTracker requestServiceTracker;
	
	BundleContext bundleContext;
	
	protected void setUp() throws Exception {
		httpAdminServiceTracker = new ServiceTracker(bundleContext,
				HttpAdminService.class.getName(), null);
		httpAdminServiceTracker.waitForService(3000);
		httpAdminServiceTracker.open();
		HttpAdminService httpAdminService = (HttpAdminService) httpAdminServiceTracker
				.getService();
		assertNotNull(httpAdminService);
		httpServerInstance = httpAdminService.createHttpServer("internal")
				.port(9090).start();

		Interceptor interceptor = new Interceptor();
		bundleContext.registerService(HttpRequestInterceptor.class.getName(),
				interceptor, null);

		requestServiceTracker = new ServiceTracker(bundleContext,
				RequestService.class.getName(), null);
		requestServiceTracker.waitForService(3000);
		requestServiceTracker.open();
		RequestService requestService = (RequestService) requestServiceTracker
				.getService();
		assertNotNull(requestService);
		interceptor.setRequestService(requestService);

		httpServiceTracker = new ServiceTracker(bundleContext,
				HttpService.class.getName(), null);
		httpServiceTracker.waitForService(3000);
		httpServiceTracker.open();
		HttpService httpService = (HttpService) httpServiceTracker.getService();
		assertNotNull(httpService);

		HttpServlet servlet = new HttpServlet() {
			@Override
			protected void doGet(HttpServletRequest req,
					HttpServletResponse resp) throws ServletException,
					IOException {
			}
		};
		HttpContext httpContext = httpService.createDefaultHttpContext();
		Dictionary initparams = new Properties();
		httpService.registerServlet("/hello", servlet, initparams, httpContext);
	}

	protected void tearDown() throws Exception {
		httpServerInstance.shutdown();
		httpAdminServiceTracker.close();
		httpServiceTracker.close();
		requestServiceTracker.close();
	}

	@Test
	public void testHttpRequestInterceptor(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		setUp();
		HttpClient httpClient = new HttpClient();
		GetMethod request = new GetMethod("http://localhost:9090/hello");
		assertEquals(200, httpClient.executeMethod(request));
		String response = request.getResponseBodyAsString();
		assertTrue(response.trim().startsWith("Before"));
		assertTrue(response.trim().endsWith("After"));
		tearDown();
	}
	
}
