package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import java.net.ConnectException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServer;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import bndtools.runtime.junit.OSGiTestCase;

public class HttpAdminServiceTest extends OSGiTestCase
{

  private HttpServerInstance externalServerInstance;

  private ServiceTracker httpAdminServiceTracker;

  private HttpServerInstance internalServerInstance;

  @Override
  protected void setUp() throws Exception
  {
    BundleContext bundleContext = getBundleContext();

    httpAdminServiceTracker = new ServiceTracker(bundleContext,
        HttpAdminService.class.getName(), null);
    httpAdminServiceTracker.waitForService(3000);
    httpAdminServiceTracker.open();
    HttpAdminService httpAdminService = (HttpAdminService) httpAdminServiceTracker
        .getService();
    assertNotNull(httpAdminService);
    internalServerInstance = httpAdminService.startServer(new HttpServer(
        "internal", 9090));
    externalServerInstance = httpAdminService.startServer(new HttpServer(
        "external", 8080));
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception
  {
    internalServerInstance.shutdown();
    externalServerInstance.shutdown();
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
