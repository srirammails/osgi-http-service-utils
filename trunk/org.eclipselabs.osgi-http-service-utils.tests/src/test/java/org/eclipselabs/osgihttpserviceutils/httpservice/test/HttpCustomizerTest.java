package org.eclipselabs.osgihttpserviceutils.httpservice.test;

import org.eclipselabs.osgihttpserviceutils.httpservice.test.utils.CustomizerProbe;
import org.eclipselabs.osgihttpserviceutils.httpservice.test.utils.HttpUtilsPaxExamn;
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
		return HttpUtilsPaxExamn.config();
	}
		
	
	@Test
    public TestAddress testCustomizerBundle( TestProbeBuilder builder )
    {
        return builder
        		.setHeader(Constants.EXPORT_PACKAGE, "org.eclipselabs.osgihttpserviceutils.jettycustomizer")
        		.addTest(CustomizerProbe.class);
    }

}
