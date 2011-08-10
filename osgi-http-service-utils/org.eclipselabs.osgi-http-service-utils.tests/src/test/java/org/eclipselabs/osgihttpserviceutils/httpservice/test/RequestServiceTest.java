package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.eclipselabs.osgihttpserviceutils.httpservice.RequestContext;
import org.eclipselabs.osgihttpserviceutils.httpservice.RequestService;
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
public class RequestServiceTest {
	
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
	
	
	class Request extends Thread {

		Throwable exp = null;

		@Override
		public void run() {
			try {
				HttpClient httpClient = new HttpClient();
				GetMethod request = new GetMethod("http://localhost:9090/hello");
				assertEquals(200, httpClient.executeMethod(request));
			} catch (Throwable exp) {
				this.exp = exp;
			}
		}
	}

	class TestServlet extends HttpServlet {

		RequestContext context;

		public void setContext(RequestContext context) {
			this.context = context;
		}

	}

	private static final String REQUEST_ID = "requestId";

	private ServiceTracker httpAdminServiceTracker;

	private HttpServerInstance httpServerInstance;

	private ServiceTracker httpServiceTracker;

	private RequestService requestService;

	private ServiceTracker requestServiceTracker;
	
	BundleContext bundleContext;

	TestServlet servlet = new TestServlet() {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			UUID requestId = UUID.randomUUID();
			context.putAttribute(REQUEST_ID, requestId.toString());
			resp.getWriter().println("Hello World");
			assertEquals(requestId.toString(), context.getAttribute(REQUEST_ID));
		};
	};

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

		requestServiceTracker = new ServiceTracker(bundleContext,
				RequestService.class.getName(), null);
		requestServiceTracker.waitForService(3000);
		requestServiceTracker.open();
		requestService = (RequestService) requestServiceTracker.getService();
		assertNotNull(requestService);

		httpServiceTracker = new ServiceTracker(bundleContext,
				HttpService.class.getName(), null);
		httpServiceTracker.waitForService(3000);
		httpServiceTracker.open();
		HttpService httpService = (HttpService) httpServiceTracker.getService();
		assertNotNull(httpService);

		servlet.setContext(requestService.getRequestContext());
		HttpContext httpContext = httpService.createDefaultHttpContext();
		Dictionary initparams = new Properties();
		httpService.registerServlet("/hello", servlet, initparams, httpContext);
	}

	protected void tearDown() throws Exception {
		httpServerInstance.shutdown();
		requestServiceTracker.close();
		httpServiceTracker.close();
		httpAdminServiceTracker.close();
	}

	@Test
	public void testRequestContext_MulitpeRequests(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		setUp();
		ArrayList<Request> requests = new ArrayList<Request>();
		for (int i = 0; i < 100; i++) {
			requests.add(new Request());
		}
		for (Request request : requests) {
			request.start();
		}
		for (Request request : requests) {
			request.join();
		}
		for (Request request : requests) {
			assertNull(request.exp);
		}
		tearDown();
	}
	
	@Test
	public void testRequestContext_RequestIdSingleRequest(BundleContext bundleContext) throws Exception {
		this.bundleContext = bundleContext;
		setUp();
		HttpClient httpClient = new HttpClient();
		GetMethod request = new GetMethod("http://localhost:9090/hello");
		assertEquals(200, httpClient.executeMethod(request));
		tearDown();
	}
	
}
