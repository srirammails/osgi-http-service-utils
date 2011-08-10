package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.scanDir;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import org.eclipselabs.osgihttpserviceutils.httpservice.test.utils.CustomizerProbe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Constants;

@RunWith(JUnit4TestRunner.class)
public class HttpCustomizerTest {
	
	@Configuration()
	public Option[] config() {
		return options(
//				uncomment for remote debugging the test
//				vmOption("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
				systemProperty("osgi.console").value("console"),
				junitBundles(),
				equinox(),
				felix(),
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
		
	
	@Test
    public TestAddress testCustomizerBundle( TestProbeBuilder builder )
    {
        return builder
        		.setHeader(Constants.EXPORT_PACKAGE, "org.eclipselabs.osgihttpserviceutils.jettycustomizer")
        		.addTest(CustomizerProbe.class);
    }

}