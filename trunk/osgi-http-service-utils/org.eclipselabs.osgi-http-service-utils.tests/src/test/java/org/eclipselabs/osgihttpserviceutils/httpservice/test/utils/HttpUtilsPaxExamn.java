package org.eclipselabs.osgihttpserviceutils.httpservice.test.utils;

import static org.ops4j.pax.exam.CoreOptions.*;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;

public class HttpUtilsPaxExamn {

	@Configuration()
	public static Option[] config() {
		return options(
//			vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
			junitBundles(),
			equinox(), 		
			provision(
				mavenBundle("org.osgi", "org.osgi.compendium", "4.2.0"),
				mavenBundle("commons-io", "commons-io", "2.0.1"),
				mavenBundle("org.slf4j", "slf4j-api", "1.6.1"),
				mavenBundle("org.slf4j", "slf4j-simple", "1.6.1"),
				mavenBundle("org.mortbay.jetty", "servlet-api", "3.0.20100224"),
				mavenBundle("org.mortbay.jetty", "jetty", "6.1.26"),
				mavenBundle("org.mortbay.jetty", "jetty-util", "6.1.26"),
				mavenBundle("org.eclipse.equinox.http", "servlet", "1.0.0-v20070606"),
				mavenBundle("org.apache.felix", "org.apache.felix.scr","1.6.0"),
				mavenBundle("org.easymock", "easymock", "3.0"),
				wrappedBundle(mavenBundle("cglib", "cglib-nodep", "2.2")),
				wrappedBundle(mavenBundle("commons-httpclient", "commons-httpclient", "3.1")),
				wrappedBundle(mavenBundle("commons-codec", "commons-codec", "1.3")),
				scanDir("../org.eclipselabs.osgi-http-service-utils.api/target").filter("*.jar"),
				scanDir("../org.eclipselabs.osgi-http-service-utils.internal/target").filter("*.jar")
			)
		);
	}
	
}
