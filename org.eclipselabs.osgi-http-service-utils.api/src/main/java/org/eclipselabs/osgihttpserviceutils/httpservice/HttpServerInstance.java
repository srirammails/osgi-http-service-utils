package org.eclipselabs.osgihttpserviceutils.httpservice;

/**
 * The interface describes a running HTTP server via this interface a HTTP
 * server can be shutdown.
 */
public interface HttpServerInstance {

	/**
	 * Shutdown a running HTTP server.
	 */
	void shutdown();
	
	/**
	 * Trigger a configuration reload of the running HTTP server instance.
	 */
	void reloadConfiguration();

}
