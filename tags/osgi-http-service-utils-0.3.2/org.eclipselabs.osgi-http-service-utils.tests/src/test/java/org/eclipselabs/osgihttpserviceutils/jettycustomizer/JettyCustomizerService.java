package org.eclipselabs.osgihttpserviceutils.jettycustomizer;

import java.util.Dictionary;

public interface JettyCustomizerService {
	
	Object customizeContext(Object context, Dictionary settings);
}
