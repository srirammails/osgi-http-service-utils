package org.eclipselabs.osgihttpserviceutils.httpservice;

import junit.framework.TestCase;

public class HttpServerTest extends TestCase {
	
	HttpServer httpServer;
	
	protected void setUp() throws Exception {
		httpServer = new HttpServer("testserver");
		System.clearProperty("org.osgi.service.http.testserver.port");
		super.setUp();
	}

	public void testSetPort() {
		httpServer.setPort(8080);
		assertEquals(8080, httpServer.getPort());
	}

	public void testGetPort_ViaSystemProperty() {
		System.setProperty("org.osgi.service.http.testserver.port", "9090");
		assertEquals(9090, httpServer.getPort());
	}
	
	public void testGetPort_NoPortConfigured() {
		try{
			httpServer.getPort();
			fail("Runtime Eception expected when no port was configured!");
		}
		catch(RuntimeException exp){
			assertTrue(true);
		}
	}

}
