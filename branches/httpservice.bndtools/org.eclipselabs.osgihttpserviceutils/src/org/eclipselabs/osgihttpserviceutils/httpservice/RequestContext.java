package org.eclipselabs.osgihttpserviceutils.httpservice;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface RequestContext
{

  Map<Object, Object> getAllAttributes();

  Object getAttribute(Object key);

  ServletRequest getRequest();

  ServletResponse getResponse();

  Object putAttribute(Object key, Object value);

  Object removeAttribute(Object key);
}
