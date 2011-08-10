package org.eclipselabs.osgihttpserviceutils.jettycustomizer;

import java.util.Dictionary;

import org.eclipselabs.osgihttpserviceutils.httpservice.JettyCustomizer;

public class JettyTestCustomizer extends JettyCustomizer {

	private static JettyCustomizerService jettyCustomizerService;
	
	@Override
	public Object customizeContext(Object context, Dictionary settings) {
		return jettyCustomizerService.customizeContext(context, settings);
	}

	public static void setJettyCustomizerService(JettyCustomizerService jettyCustomizerService) {
		JettyTestCustomizer.jettyCustomizerService = jettyCustomizerService;
	}

}
