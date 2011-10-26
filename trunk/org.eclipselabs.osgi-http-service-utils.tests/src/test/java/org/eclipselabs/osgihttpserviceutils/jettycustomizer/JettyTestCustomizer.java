package org.eclipselabs.osgihttpserviceutils.jettycustomizer;

import java.util.Dictionary;

import org.eclipselabs.osgihttpserviceutils.httpservice.JettyCustomizer;
import org.mortbay.jetty.AbstractConnector;

public class JettyTestCustomizer extends JettyCustomizer {

	private static JettyCustomizerService jettyCustomizerService;
	
	@Override
	public Object customizeContext(Object context, Dictionary settings) {
		return jettyCustomizerService.customizeContext(context, settings);
	}

	@Override
	public Object customizeHttpConnector(Object connector, Dictionary settings) {
		if (connector instanceof AbstractConnector) {
			AbstractConnector jettyConnector = (AbstractConnector) connector;
			jettyConnector.setPort(8080);
		}
		return connector;
	}
	
	public static void setJettyCustomizerService(JettyCustomizerService jettyCustomizerService) {
		JettyTestCustomizer.jettyCustomizerService = jettyCustomizerService;
	}

}
