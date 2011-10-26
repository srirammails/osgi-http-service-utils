package org.eclipselabs.osgihttpserviceutils.httpservice.test.utils;

import static org.junit.Assert.assertNotNull;
import static org.easymock.EasyMock.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.eclipselabs.osgihttpserviceutils.jettycustomizer.JettyCustomizerService;
import org.eclipselabs.osgihttpserviceutils.jettycustomizer.JettyTestCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class CustomizerProbe {

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
		startHttpServiceWithCustomizer(bundleContext);
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

	private void startHttpServiceWithCustomizer(BundleContext bundleContext) throws Exception {

		String customizeClassPropertyName = "org.eclipselabs.osgihttpserviceutils.httpservice.customizer.class";
		System.setProperty(customizeClassPropertyName, JettyTestCustomizer.class.getName());

		ServiceTracker httpAdminServiceTracker = new ServiceTracker(bundleContext, HttpAdminService.class.getName(), null);
		httpAdminServiceTracker.waitForService(3000);
		httpAdminServiceTracker.open();
		HttpAdminService httpAdminService = (HttpAdminService) httpAdminServiceTracker.getService();
		assertNotNull(httpAdminService);

		Map<String, String> serviceProperties = new HashMap<String, String>();
		serviceProperties.put("external.http.service", "false");
		HttpServerInstance internalServerInstance = httpAdminService
				.createHttpServer("internal")
				.serviceProperties(serviceProperties).port(9090).start();

		internalServerInstance.shutdown();
		httpAdminServiceTracker.close();
	}

}
