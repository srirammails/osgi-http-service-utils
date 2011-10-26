package org.eclipselabs.osgihttpserviceutils.httpservice.test.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.easymock.EasyMock.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Dictionary;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.eclipselabs.osgihttpserviceutils.jettycustomizer.JettyCustomizerService;
import org.eclipselabs.osgihttpserviceutils.jettycustomizer.JettyTestCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class CustomizerConnectorProbe {

	JettyCustomizerService mockCustomizerService;

	public void probe(BundleContext bundleContext) throws Exception {
		final Capture<Object> contextCapture = new Capture<Object>();
		mockCustomizerService = createMock(JettyCustomizerService.class);
		expect(
				mockCustomizerService.customizeContext(capture(contextCapture),
						anyObject(Dictionary.class))).andAnswer(
				answer(contextCapture));
		replay(mockCustomizerService);
		JettyTestCustomizer.setJettyCustomizerService(mockCustomizerService);
		
		startAndVerifyHttpServiceWithCustomizer(bundleContext);
		
		verify(mockCustomizerService);
		reset(mockCustomizerService);
	}

	public IAnswer<Object> answer(final Capture<Object> capture) {
		return new IAnswer<Object>() {
			@Override
			public Object answer() throws Throwable {
				return capture.getValue();
			}
		};
	}

	void startAndVerifyHttpServiceWithCustomizer(BundleContext bundleContext) throws Exception {

		String customizeClassPropertyName = "org.eclipselabs.osgihttpserviceutils.httpservice.customizer.class";
		System.setProperty(customizeClassPropertyName, JettyTestCustomizer.class.getName());

		ServiceTracker httpAdminServiceTracker = new ServiceTracker(bundleContext, HttpAdminService.class.getName(), null);
		httpAdminServiceTracker.waitForService(3000);
		httpAdminServiceTracker.open();
		HttpAdminService httpAdminService = (HttpAdminService) httpAdminServiceTracker.getService();
		assertNotNull(httpAdminService);

		// Setup server via jetty XML configuration
		String jettyConfigurationTemplate = "/org/eclipselabs/osgihttpserviceutils/httpservice/test/jetty.xml";
		InputStream resourceAsStream = getClass().getResourceAsStream(jettyConfigurationTemplate);
		String tmpDir = System.getProperty("java.io.tmpdir");
		IOUtils.copy(resourceAsStream, new FileOutputStream(new File(tmpDir, "jetty-sample-jetty.xml")));
		System.setProperty("jetty.server.configuration.directory", tmpDir);
		HttpServerInstance internalServerInstance = httpAdminService.createHttpServer("jetty-sample").start();
		
		// Verify that the port is 8080 not 8090
		HttpClient httpClient = new HttpClient();
		GetMethod externalRequest = new GetMethod("http://localhost:8080");
		assertEquals("Port must be 8080 because of customize.", 404, httpClient.executeMethod(externalRequest));
		
		internalServerInstance.shutdown();
		httpAdminServiceTracker.close();
	}

}
