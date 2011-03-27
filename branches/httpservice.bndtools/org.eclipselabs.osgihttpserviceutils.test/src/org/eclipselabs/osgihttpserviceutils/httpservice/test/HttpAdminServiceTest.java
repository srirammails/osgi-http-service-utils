package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ConnectException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServer;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import bndtools.runtime.junit.OSGiTestCase;

public class HttpAdminServiceTest extends OSGiTestCase
{

  private ServiceTracker httpAdminServiceTracker;
  
  private HttpServerInstance externalServerInstance;

  private HttpServerInstance internalServerInstance;
  
  private HttpServerInstance jettyXmlServerInstance;

  @Override
  protected void setUp() throws Exception
  {
    BundleContext bundleContext = getBundleContext();

    httpAdminServiceTracker = new ServiceTracker(bundleContext,
        HttpAdminService.class.getName(), null);
    httpAdminServiceTracker.waitForService(3000);
    httpAdminServiceTracker.open();
    HttpAdminService httpAdminService = (HttpAdminService) httpAdminServiceTracker.getService();
    assertNotNull(httpAdminService);
    
    // Setup server port direct in Java code
    internalServerInstance = httpAdminService.startServer(new HttpServer("internal", 9090));
    
    // Setup server port via system property
    System.setProperty("org.osgi.service.http.external.port", "8080");
    externalServerInstance = httpAdminService.startServer(new HttpServer("external"));
    
    // Setup server via jetty XML configuration
    InputStream resourceAsStream = getClass().getResourceAsStream("jetty.xml");
    String tmpDir = System.getProperty("java.io.tmpdir");
    IOUtils.copy(resourceAsStream, new FileOutputStream(new File(tmpDir, "jetty-sample-jetty.xml")));
    System.setProperty("jetty.server.configuration.directory", tmpDir);
    jettyXmlServerInstance = httpAdminService.startServer(new HttpServer("jetty-sample"));
    
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception
  {
    internalServerInstance.shutdown();
    externalServerInstance.shutdown();
    jettyXmlServerInstance.shutdown();
    httpAdminServiceTracker.close();
    super.tearDown();
  }

  public void testInternalAndExternalHttpService() throws Exception
  {
    HttpClient httpClient = new HttpClient();
    GetMethod internalRequest = new GetMethod("http://localhost:9090/hello");
    assertEquals(404, httpClient.executeMethod(internalRequest));

    GetMethod externalRequest = new GetMethod("http://localhost:8080/hello");
    assertEquals(404, httpClient.executeMethod(externalRequest));
    
    GetMethod jettyXmlRequest = new GetMethod("http://localhost:8090/hello");
    assertEquals(404, httpClient.executeMethod(externalRequest));
  }

  public void testShutdownHttpServer() throws Exception
  {
    HttpClient httpClient = new HttpClient();
    GetMethod internalRequest = new GetMethod("http://localhost:9090/hello");
    assertEquals(404, httpClient.executeMethod(internalRequest));
    internalServerInstance.shutdown();
    try
    {
      httpClient.executeMethod(internalRequest);
      fail("Connection refused expected!");
    }
    catch (ConnectException exp)
    {
      assertTrue(true);
    }
  }

}
