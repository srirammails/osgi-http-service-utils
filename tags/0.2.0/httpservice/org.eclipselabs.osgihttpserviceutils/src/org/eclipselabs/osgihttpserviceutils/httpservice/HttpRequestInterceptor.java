package org.eclipselabs.osgihttpserviceutils.httpservice;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * Intercepter for the HTTP javax.servlet chain.
 */
public interface HttpRequestInterceptor {

	/**
	 * Method is invoked after the request processing.
	 * 
	 */
	public void afterRequest() throws IOException, ServletException;

	/**
	 * Method is invoked before the request chain starts.
	 * 
	 */
	public void beforeRequest() throws IOException, ServletException;

}
