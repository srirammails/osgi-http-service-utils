package org.eclipselabs.osgihttpserviceutils.example;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component
public class HttpServerDemo {

	private HttpAdminService httpAdminService;

	private HttpServerInstance httpServerInstance;

	@Activate
	public void activate() {
		httpServerInstance = httpAdminService.createHttpServer("default")
				.port(8080).start();
	}

	@Deactivate
	public void deactivate() {
		httpServerInstance.shutdown();
	}

	@Reference
	public void setHttpAdminService(HttpAdminService httpAdminService) {
		this.httpAdminService = httpAdminService;
	}

}
