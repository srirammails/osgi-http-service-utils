package org.eclipselabs.osgihttpserviceutils.httpservice.demo.requestinterceptor;

import java.io.IOException;

import javax.servlet.ServletException;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpRequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Component;

@Component
public class HttpRequestInterceptorDemo implements HttpRequestInterceptor {

	private static final Logger logger = LoggerFactory
			.getLogger(HttpRequestInterceptorDemo.class);
	
	@Override
	public void beforeRequest() throws IOException, ServletException {
		logger.info("Before request interceptor was invoked !!!");
	}
	
	@Override
	public void afterRequest() throws IOException, ServletException {
		logger.info("After request interceptor was invoked !!!");
	}

}
