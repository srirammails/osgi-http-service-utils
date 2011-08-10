package org.eclipselabs.osgihttpserviceutils.httpservice;

import junit.framework.TestCase;

import org.eclipselabs.osgihttpserviceutils.httpservice.internal.HttpService;

public class HttpAdminServiceTest extends TestCase {
	
	HttpAdminService httpAdminService;

	@Override
	protected void setUp() throws Exception {
		httpAdminService = new HttpService();
		super.setUp();
	}

	public void testCreateHttpServerString() {
		String symbolicName = "internal";
		HttpServer httpServer = httpAdminService.createHttpServer(symbolicName);
		assertEquals(symbolicName, httpServer.getSymbolicName());
		httpServer = httpAdminService.createHttpServer(symbolicName);
		assertEquals(symbolicName, httpServer.getSymbolicName());
	}

}
