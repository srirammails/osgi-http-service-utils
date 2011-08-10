package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

public class HttpServiceInternalException extends RuntimeException {

	public HttpServiceInternalException(String msg) {
		super(msg);
	}

	public HttpServiceInternalException(Throwable exp) {
		super(exp);
	}

}
