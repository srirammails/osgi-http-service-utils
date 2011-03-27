package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import java.io.IOException;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpRequestInterceptor;
import org.eclipselabs.osgihttpserviceutils.httpservice.RequestService;

public class Interceptor implements HttpRequestInterceptor {

  private RequestService requestService;

  @Override
	public boolean afterRequest() {
		try {
			requestService.getRequestContext().getResponse().getWriter()
					.println("After");
		} catch (IOException exp) {
			throw new RuntimeException(exp);
		}
		return false;
	}

  @Override
	public boolean beforeRequest() {
		try {
			requestService.getRequestContext().getResponse().getWriter()
					.println("Before");
		} catch (IOException exp) {
			throw new RuntimeException(exp);
		}
		return false;
	}

  public void setRequestService(RequestService requestService) {
		this.requestService = requestService;
	}

}