package org.eclipselabs.osgihttpserviceutils.httpservice.demo;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component
public class HTTPAdminDemo {
	
	private static final Logger logger = LoggerFactory.getLogger(HTTPAdminDemo.class);

	private HttpAdminService httpAdminService;
	
	private HttpServerInstance httpServerInstance;

	@Reference
	public void setHttpAdminService(HttpAdminService httpAdminService) {
		this.httpAdminService = httpAdminService;
	}
	
	@Activate
	public void start() {
		logger.info("Start HTTP OSGi service on port 8080.");
        httpServerInstance = 
        		httpAdminService
                        .createHttpServer("default")
                        .serviceProperty("http.default.service", "true")
                        .port(8080)
                        .start();
	}
	
	@Deactivate
	public void stop() {
		 logger.info("Shutdown HTTP server instance (8080).");
		 httpServerInstance.shutdown();
	}

}
