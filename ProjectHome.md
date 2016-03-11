# OSGi HTTP Service and Utils #

This project provides a HTTP Admin Service which makes it possible to start more than one OSGi HTTP Service in one platform.
The source code is based on the equinox jetty bundle (http://www.eclipse.org/equinox/).

The project extends the HTTP equinox service (source) in the following ways:

  * A OSGi HTTP Admin Service to start multiple OSGi HTTP Services in one OSGi platform.
  * Request interception logic (HTTP filters).
  * A request context which holds data for servlet request in a request scope
  * HTTP Service configuration via Jetty XML files.

## Download Now from Maven Centeral ##

http://repo1.maven.org/maven2/org/eclipselabs/osgi-http-service-utils/

## Requiered OSGi Bundles ##
The HTTP Utils bundles requires the following 3rd party bundels:
  * osgi.cmpn (4.2.1)
  * slf4j API (1.6.1)
  * slf4j Logger Implementation (e.g. logback 0.9.25)
  * OSGi DS Implementation - org.eclipse.equinox.ds (1.2.1)
  * javax.servlet (2.5.0)
  * org.eclipse.equinox.http.servlet (1.1.0)
  * org.mortbay.jetty.util (6.1.23)
  * org.mortbay.jetty.server (6.1.23)

Have a look at the distribution ZIP archive in the download area.

## Examples and Sample Usage ##

...Doku is coming soon...

See also the demo project in the SVN [here](http://code.google.com/a/eclipselabs.org/p/osgi-http-service-utils/source/browse/#svn%2Ftrunk%2Forg.eclipselabs.osgi-http-service-utils.demo)


# Eclipse Public License #
The project is published under the terms of the EPL - the Eclipse Public License - v 1.0
see http://www.eclipse.org/legal/epl-v10.html