package org.eclipselabs.osgihttpserviceutils.httpservice;

/**
 * Intercepter for the HTTP javax.servlet chain.
 */
public interface HttpRequestInterceptor
{

  /**
   * Method is invoked after the request processing.
   * 
   * @return when false the next request interceptor will be invoked
   *         or the response is send to the client.
   */
  public boolean afterRequest();

  /**
   * Method is invoked before the request chain starts.
   * 
   * @return when false the next request interceptor will be invoked
   *         or the request processing starts.
   */
  public boolean beforeRequest();

}
