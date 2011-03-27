package org.eclipselabs.osgihttpserviceutils.httpservice;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * The request context can be used to add object in a request scope. 
 * Each HTTP request has its own request context.
 */
public interface RequestContext
{

  Map<Object, Object> getAllAttributes();

  Object getAttribute(Object key);

  ServletRequest getRequest();

  ServletResponse getResponse();

  Object putAttribute(Object key, Object value);

  Object removeAttribute(Object key);
}
