package org.eclipselabs.osgihttpserviceutils.httpservice;

import junit.framework.TestCase;

import org.eclipselabs.osgihttpserviceutils.httpservice.internal.HttpService;

public class HttpServerTest extends TestCase {
	
	HttpAdminService httpAdminService;

	HttpServer httpServer;
	
	@Override
	protected void setUp() throws Exception {
		httpAdminService = new HttpService();
		httpServer = httpAdminService.createHttpServer("testserver").port(8080);
		super.setUp();
	}

	public void testGetHttpPort() {
		assertEquals(8080, httpServer.getPort());
	}

}
