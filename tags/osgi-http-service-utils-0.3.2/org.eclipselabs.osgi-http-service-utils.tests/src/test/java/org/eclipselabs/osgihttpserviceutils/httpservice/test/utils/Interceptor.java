package org.eclipselabs.osgihttpserviceutils.httpservice.test.utils;

import java.io.IOException;

import javax.servlet.ServletException;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpRequestInterceptor;
import org.eclipselabs.osgihttpserviceutils.httpservice.RequestService;

public class Interceptor implements HttpRequestInterceptor {

	private RequestService requestService;

	@Override
	public void afterRequest() throws IOException, ServletException {
		requestService.getRequestContext().getResponse().getWriter()
				.println("After");
	}

	@Override
	public void beforeRequest() throws IOException, ServletException {
		requestService.getRequestContext().getResponse().getWriter()
				.println("Before");
	}

	public void setRequestService(RequestService requestService) {
		this.requestService = requestService;
	}

}