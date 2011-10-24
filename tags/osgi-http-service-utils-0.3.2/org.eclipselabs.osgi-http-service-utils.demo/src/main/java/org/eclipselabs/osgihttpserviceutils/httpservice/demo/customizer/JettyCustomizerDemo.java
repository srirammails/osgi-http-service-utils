package org.eclipselabs.osgihttpserviceutils.httpservice.demo.customizer;

import java.util.Dictionary;

import org.eclipselabs.osgihttpserviceutils.httpservice.JettyCustomizer;
import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.RequestLog;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.servlet.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyCustomizerDemo extends JettyCustomizer {

	private static final Logger logger = LoggerFactory
			.getLogger(JettyCustomizerDemo.class);

	class SimpleRequestLog extends AbstractLifeCycle implements RequestLog {
		@Override
		public void log(Request request, Response response) {
			logger.info("HTTP : " + request.getMethod() + " : "
					+ request.getPathInfo());
		}
	}

	@Override
	public Object customizeContext(Object context, Dictionary settings) {
		logger.info("customizeContext() - demo");
		if (context instanceof Context) {
			Context jettyContext = (Context) context;
			RequestLogHandler requestLogHandler = new RequestLogHandler();
			requestLogHandler.setRequestLog(new SimpleRequestLog());
			jettyContext.addHandler(requestLogHandler);

		} else {
			logger.warn("Given context object was not an Instance of org.mortbay.jetty.servlet.Context.");
		}
		return super.customizeContext(context, settings);
	}

	@Override
	public Object customizeHttpConnector(Object connector, Dictionary settings) {
		logger.info("customizeHttpConnector() - demo");
		if (connector instanceof AbstractConnector) {
			logger.info("Customize the HTTP OSGi service set port to 9090.");
			AbstractConnector jettyConnector = (AbstractConnector) connector;
			jettyConnector.setPort(9090);
		} else {
			logger.warn("Given connector object was not an Instance of org.mortbay.jetty.AbstractConnector.");
		}
		return super.customizeHttpConnector(connector, settings);
	}

}
