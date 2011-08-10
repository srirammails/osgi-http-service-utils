package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipselabs.osgihttpserviceutils.httpservice.*;
import org.eclipselabs.osgihttpserviceutils.httpservice.test.customizer.JettyTestCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import bndtools.runtime.junit.OSGiTestCase;

public class HttpCustomizerTest extends OSGiTestCase
{
  
  private HttpServerInstance internalServerInstance;
  
  private ServiceTracker httpAdminServiceTracker;
  
  @Override
  protected void setUp() throws Exception
  {
    BundleContext bundleContext = getBundleContext();
    
    JettyTestCustomizer.customizeContextWasCalled = false;
    String customizeClassPropertyName = "org.eclipselabs.osgihttpserviceutils.httpservice.customizer.class";
    System.setProperty(customizeClassPropertyName, JettyTestCustomizer.class.getName());
    
    httpAdminServiceTracker = new ServiceTracker(bundleContext,
        HttpAdminService.class.getName(), null);
    httpAdminServiceTracker.waitForService(3000);
    httpAdminServiceTracker.open();
    HttpAdminService httpAdminService = (HttpAdminService) httpAdminServiceTracker
        .getService();
    assertNotNull(httpAdminService);
    
    Map<String, String> serviceProperties = new HashMap<String, String>();
    serviceProperties.put("external.http.service", "false");
    internalServerInstance = httpAdminService.createHttpServer("internal")
        .serviceProperties(serviceProperties).port(9090).start();
    
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception
  {
    internalServerInstance.shutdown();
    httpAdminServiceTracker.close();
    super.tearDown();
  }
  
  public void testCustomizer()
  {
    assertTrue(JettyTestCustomizer.customizeContextWasCalled);
  }
  
}
