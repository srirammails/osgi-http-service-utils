package org.eclipselabs.osgihttpserviceutils.example;

import java.util.Properties;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpAdminService;
import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServerInstance;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {
	
	static final Logger logger = LoggerFactory.getLogger(Activator.class);

	ServiceTracker httpAdminServiceTracker;
	ServiceTracker defaultHttpServiceTracker;
	ServiceTracker internalHttpServiceTracker;

	HttpService defaultHttpService;
	HttpService internalHttpService;

	HttpServerInstance defaultHttpServerInstance;
	HttpServerInstance internalHttpServerInstance;

	@Override
	public void start(BundleContext context) throws Exception {
		logger.info("Get HTTP Admin Service");
		HttpAdminService httpAdminService = getHttpAdminService(context);

		logger.info("Start default HTTP OSGi service on port 8080.");
		defaultHttpServerInstance = httpAdminService
				.createHttpServer("default")
				.serviceProperty("http.default.service", "true")
				.port(8080)
				.start();

		logger.info("Start internal HTTP OSGi service on port 9090.");
		internalHttpServerInstance = httpAdminService
				.createHttpServer("internal")
				.serviceProperty("http.default.service", "false")
				.port(9090)
				.start();

		logger.info("Get default HTTP Service.");
		defaultHttpService = getDefaultHttpService(context);
		
		logger.info("Default HTTP service register demo servlet, " 
				+ "invoke demo servlet under http://localhost:8080/.");
		defaultHttpService.registerServlet("/", 
				new EchoServlet("Hello Default HTTP Service"), new Properties(), null);
		
		logger.info("Get internal HTTP Service.");
		internalHttpService = getInternalHttpService(context);
		
		logger.info("Internal HTTP service register demo servlet, " 
				+ "invoke demo servlet under http://localhost:9090/.");
		internalHttpService.registerServlet("/", 
				new EchoServlet("Hello Internal HTTP Service"), new Properties(), null);
	}
	
	@Override
	public void stop(BundleContext context) throws Exception {
		logger.info("Default HTTP Service unregister demo servlet from path /.");
		defaultHttpService.unregister("/");
		
		logger.info("Internal HTTP Service unregister demo servlet from path /.");
		internalHttpService.unregister("/");
		
		logger.info("Shutdown default HTTP server instance (8080).");
		defaultHttpServerInstance.shutdown();
		
		logger.info("Shutdown internal HTTP server instance (9090).");
		internalHttpServerInstance.shutdown();
		
		httpAdminServiceTracker.close();
		defaultHttpServiceTracker.close();
		internalHttpServiceTracker.close();
	}

	 HttpService getInternalHttpService(BundleContext context) throws Exception {
		String filterString = "(&(" + Constants.OBJECTCLASS + "="
				+ HttpService.class.getName() + ")(http.default.service=false))";
		logger.info("HTTP Service Filter: " + filterString);
		Filter filter = context.createFilter(filterString);
		internalHttpServiceTracker = new ServiceTracker(context, filter, null);
		internalHttpServiceTracker.waitForService(3000);
		internalHttpServiceTracker.open();
		return (HttpService) internalHttpServiceTracker.getService();
	}

	HttpService getDefaultHttpService(BundleContext context) throws Exception {
		String filterString = "(&(" + Constants.OBJECTCLASS + "="
				+ HttpService.class.getName() + ")(http.default.service=true))";
		logger.info("HTTP Service Filter: " + filterString);
		Filter filter = context.createFilter(filterString);
		defaultHttpServiceTracker = new ServiceTracker(context, filter, null);
		defaultHttpServiceTracker.waitForService(3000);
		defaultHttpServiceTracker.open();
		return (HttpService) defaultHttpServiceTracker.getService();
	}

	HttpAdminService getHttpAdminService(BundleContext context) throws Exception {
		httpAdminServiceTracker = new ServiceTracker(context,
				HttpAdminService.class.getName(), null);
		httpAdminServiceTracker.waitForService(3000);
		httpAdminServiceTracker.open();
		HttpAdminService httpAdminService = (HttpAdminService) httpAdminServiceTracker
				.getService();
		return httpAdminService;
	}

}
