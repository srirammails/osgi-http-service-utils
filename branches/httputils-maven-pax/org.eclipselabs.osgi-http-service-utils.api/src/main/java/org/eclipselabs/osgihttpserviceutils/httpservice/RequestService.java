package org.eclipselabs.osgihttpserviceutils.httpservice;

/**
 * The request service to query the request context.
 */
public interface RequestService {

	/**
	 * Returns the actual request context.
	 * 
	 * @return the actual request context not null.
	 */
	RequestContext getRequestContext();

}
