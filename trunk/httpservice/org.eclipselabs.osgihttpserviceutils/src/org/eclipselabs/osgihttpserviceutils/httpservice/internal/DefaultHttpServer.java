package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServer;

public abstract class DefaultHttpServer implements HttpServer {

	private int httpPort = 0;

	private final Map<Object, Object> serviceProps = new HashMap<Object, Object>();

	private final String symbolicName;

	public DefaultHttpServer(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	@Override
	public int getPort() {
		return httpPort;
	}

	@Override
	public Map<Object, Object> getServiceProperties() {
		return serviceProps;
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
	public HttpServer serviceProperties(Map<?, ?> serviceProperties) {
		serviceProps.putAll(serviceProperties);
		return this;
	}

	@Override
	public HttpServer serviceProperty(Object key, Object value) {
		serviceProps.put(key, value);
		return this;
	}

	@Override
	public void setPort(int port) {
		this.httpPort = port;
	}

}
