package org.eclipselabs.osgihttpserviceutils.httpservice;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * The request context can be used to add object in a request scope. Each HTTP
 * request has its own request context.
 */
public interface RequestContext {

	/**
	 * Returns all attributes of the request context.
	 * 
	 * @return the attributes of the request context.
	 */
	Map<Object, Object> getAllAttributes();

	/**
	 * Returns a value of a attribute.
	 * 
	 * @param key
	 *            The key of the attribute.
	 * @return the value of the attribute or NULL.
	 */
	Object getAttribute(Object key);

	/**
	 * The actual servlet request.
	 * 
	 * @return the servlet request or NULL when the method is invoked outside of
	 *         a HTTP request.
	 */
	ServletRequest getRequest();

	/**
	 * The actual servlet response.
	 * 
	 * @return the servlet response or NULL when the method is invoked outside
	 *         of a HTTP request.
	 */
	ServletResponse getResponse();

	/**
	 * Add a attribute (key value pair) in the request context.
	 * 
	 * @param key
	 *            the name of the attribute
	 * @param value
	 *            the value of the attribute
	 */
	void putAttribute(Object key, Object value);

	/**
	 * Remove a attribute form the request context.
	 * 
	 * @param key
	 *            the name of the attribute.
	 * @return the value of the removed attribute.
	 */
	Object removeAttribute(Object key);
}
