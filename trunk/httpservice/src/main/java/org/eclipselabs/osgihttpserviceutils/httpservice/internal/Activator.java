package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
    
    private HttpServiceActivator httpServiceActivator;
    
    @Override
    public void start(BundleContext context) throws Exception {
        httpServiceActivator = new HttpServiceActivator();
        httpServiceActivator.start(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        httpServiceActivator.stop(context);
        httpServiceActivator = null;
    }

}
