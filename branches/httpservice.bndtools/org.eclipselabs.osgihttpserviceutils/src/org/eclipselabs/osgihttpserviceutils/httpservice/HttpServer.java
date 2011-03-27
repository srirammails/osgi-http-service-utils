package org.eclipselabs.osgihttpserviceutils.httpservice;

/**
 * 
 */
public interface HttpServer {

	int getPort();

	String getSymbolicName();

	HttpServer port(int httpPort);
	
	void setPort(int port);

	/**
	 * Start a HTTP server in the platform.
	 * 
	 * @param httpServer
	 *            describes the HTTP server to be started.
	 * @return the HTTP server instance which is running not null.
	 */
	HttpServerInstance start();

}
