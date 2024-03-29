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
import org.eclipselabs.osgihttpserviceutils.httpservice.test.utils.HttpUtilsPaxExamn;
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
		return HttpUtilsPaxExamn.config();
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
	public void testHttpRequestInterceptor(BundleContext bundleContext)
			throws Exception {
		this.bundleContext = bundleContext;
		setUp();
		try {
			HttpClient httpClient = new HttpClient();
			GetMethod request = new GetMethod("http://localhost:9090/hello");
			assertEquals(200, httpClient.executeMethod(request));
			String response = request.getResponseBodyAsString();
			assertTrue(response.trim().startsWith("Before"));
			assertTrue(response.trim().endsWith("After"));
		} finally {
			tearDown();
		}
	}

}
