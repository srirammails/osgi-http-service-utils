package com.github.tux2323.osgi.httpservice;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Intercepter for the HTTP javax.servlet chain.
 */
public interface HttpRequestInterceptor
{

  /**
   * Method is invoked before the request chain starts.
   * 
   * @param req the javax.servlet http request.
   * @param res the javax.servlet http response.
   * @return when true the next request interceptor will be invoked or
   *         the request processing starts.
   */
  public boolean beforeRequest(ServletRequest req, ServletResponse res);

  /**
   * Method is invoked after the request processing.
   * 
   * @param req the javax.servlet http request.
   * @param res the javax.servlet http response
   * @return when true the next request interceptor will be invoked or
   *         the response is send to the client.
   */
  public boolean afterRequest(ServletRequest req, ServletResponse res);

}
