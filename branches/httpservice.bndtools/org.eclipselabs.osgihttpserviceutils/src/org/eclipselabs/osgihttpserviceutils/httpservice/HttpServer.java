package org.eclipselabs.osgihttpserviceutils.httpservice;

public class HttpServer {

	private final String name;

	private int port = 0;

	public HttpServer(String name) {
		this.name = name;
	}

	public HttpServer(String name, int port) {
		this.name = name;
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	public int getPort(){
		return this.port;
	}

}
