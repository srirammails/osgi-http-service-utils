package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServer;

public abstract class DefaultHttpServer implements HttpServer {

	private int httpPort = 0;

	private final String symbolicName;

	public DefaultHttpServer(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	@Override
	public int getPort() {
		return httpPort;
	}

	@Override
	public String getSymbolicName() {
		return symbolicName;
	}

	@Override
	public HttpServer port(int httpPort) {
		setPort(httpPort);
		return this;
	}

	@Override
	public void setPort(int port) {
		this.httpPort = port;
	}

}
