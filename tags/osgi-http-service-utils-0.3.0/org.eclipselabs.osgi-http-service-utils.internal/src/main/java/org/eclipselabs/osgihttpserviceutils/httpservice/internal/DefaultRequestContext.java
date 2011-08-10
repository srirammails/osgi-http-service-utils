package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipselabs.osgihttpserviceutils.httpservice.RequestContext;

public class DefaultRequestContext implements RequestContext {

	final ThreadLocal<HashMap<Object, Object>> requestContextLocal = new ThreadLocal<HashMap<Object, Object>>();

	final ThreadLocal<ServletRequest> requestLocal = new ThreadLocal<ServletRequest>();

	final ThreadLocal<ServletResponse> responseLocal = new ThreadLocal<ServletResponse>();

	@Override
	public Map<Object, Object> getAllAttributes() {
		return requestContextLocal.get();
	}

	@Override
	public Object getAttribute(Object key) {
		return requestContextLocal.get().get(key);
	}

	@Override
	public ServletRequest getRequest() {
		return requestLocal.get();
	}

	@Override
	public ServletResponse getResponse() {
		return responseLocal.get();
	}

	@Override
	public void putAttribute(Object key, Object value) {
		requestContextLocal.get().put(key, value);
	}

	@Override
	public Object removeAttribute(Object key) {
		return requestContextLocal.get().remove(key);
	}

	public void reset() {
		requestContextLocal.set(new HashMap<Object, Object>());
		requestLocal.set(null);
		responseLocal.set(null);
	}

	public void setRequest(ServletRequest request) {
		requestLocal.set(request);
	}

	public void setResponse(ServletResponse response) {
		responseLocal.set(response);
	}

}
