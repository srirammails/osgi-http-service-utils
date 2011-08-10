package org.eclipselabs.osgihttpserviceutils.httpservice;

/**
 * A service to manage the HTTP servers in a OSGi platform. One HTTP server
 * provides one OSGi HTTP service. The HTTP server can be configured via a Jetty
 * XML file see {@link HttpServer} and for a simple mode via a set of system
 * properties see also {@link HttpServer}.
 */
public interface HttpAdminService {

	/**
	 * Create a new HTTP server in the platform.
	 * 
	 * @param symbolicName
	 *            the symbolic name for the HTTP server, the name should be
	 *            unique.
	 * @return the HTTP server not null.
	 */
	HttpServer createHttpServer(String symbolicName);

}
