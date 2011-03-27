package org.eclipselabs.osgihttpserviceutils.httpservice;

/**
 * The HTTP server which provides one OSGi HTTP service. The server can be
 * configured via a Jetty XML configuration file. The configuration file must
 * have the name ${symbolicName}-jetty.xml and the file should be in the
 * directory which can be configured via the system property
 * jetty.server.configuration.directory.
 * 
 * For a simple mode the HTTP port can be configured direct or via a system
 * property org.osgi.service.http.${symbolicName}.port or via the setter
 * setPort(...) in this class. Then the http service use a very simple
 * configuration and only the port can be setup.
 */
public interface HttpServer {

	/**
	 * The port of the HTTP service. When the port is 0 the Jetty XML
	 * configuration or the system property
	 * org.osgi.service.http.${symbolicName}.port is used for the port.
	 * 
	 * @return 0 or the port is set.
	 */
	int getPort();

	/**
	 * The symbolic name of the server, the HTTP server has a symbolic name
	 * which is unique.
	 * 
	 * @return the symbolic name not null.
	 */
	String getSymbolicName();

	/**
	 * Method to set the HTTP port in a fluent API API style.
	 * 
	 * @param httpPort
	 *            the port for the HTTP service of the HTTP server.
	 * @return this HTTP server instance not null.
	 */
	HttpServer port(int httpPort);

	/**
	 * Setter for the HTTP port. The port should not be used.
	 * 
	 * @param port
	 *            the port for the HTTP services of the HTTP server.
	 */
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
