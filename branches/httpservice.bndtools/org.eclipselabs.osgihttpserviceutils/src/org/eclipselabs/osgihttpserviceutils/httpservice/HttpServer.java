package org.eclipselabs.osgihttpserviceutils.httpservice;

public class HttpServer
{

  private String name;

  private int port;

  public HttpServer(String name, int port)
  {
    this.name = name;
    this.port = port;
  }

  public String getName()
  {
    return name;
  }

  public int getPort()
  {
    return port;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public void setPort(int port)
  {
    this.port = port;
  }

}
