package org.eclipselabs.osgihttpserviceutils.httpservice;

/**
 * A service to administrate the HTTP servers in one OSGi platform.
 *
 */
public interface HttpAdminService {

	HttpServer createHttpServer(String symbolicName);
	
}
