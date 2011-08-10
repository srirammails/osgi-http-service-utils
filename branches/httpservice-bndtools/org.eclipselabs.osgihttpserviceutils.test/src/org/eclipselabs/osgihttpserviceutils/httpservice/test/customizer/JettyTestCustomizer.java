package org.eclipselabs.osgihttpserviceutils.httpservice.test.customizer;

import java.util.Dictionary;

import org.eclipselabs.osgihttpserviceutils.httpservice.JettyCustomizer;


public class JettyTestCustomizer extends JettyCustomizer
{
  
  public static boolean customizeContextWasCalled = false;
  
  @Override
  public Object customizeContext(Object context, Dictionary settings)
  {
    customizeContextWasCalled = true;
    return context;
  }
}
