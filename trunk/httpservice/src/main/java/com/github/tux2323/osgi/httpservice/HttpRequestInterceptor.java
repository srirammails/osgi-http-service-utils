package com.github.tux2323.osgi.httpservice;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Intercepter for the HTTP javax.servlet chain.
 */
public interface HttpRequestInterceptor
{

  /** 
   * Method is invokes before the request starts.
   * @param req servlet http request.
   * @param res servket http response
   * @return when true the next request interceptor will be invoked or the request servlet.
   */
  public boolean beforeRequest(ServletRequest req, ServletResponse res);

  /** 
   * Method is invokes before the request starts.
   * @param req servlet http request.
   * @param res servket http response
   * @return when true the next request interceptor will be invoked or the response is send to the client.
   */
  public boolean afterRequest(ServletRequest req, ServletResponse res);

}
