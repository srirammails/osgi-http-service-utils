package org.eclipselabs.osgihttpserviceutils.httpservice.demo.httpservice;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Action;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

@Component
public class HttpServiceDemo {

	private static final Logger logger = LoggerFactory.getLogger(HttpServiceDemo.class);
	
	private HttpService defaultHttpService;

	class DemoServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			logger.info("Process request in Demo Servlet !!!");
			PrintWriter writer = resp.getWriter();
			writer.append("<html>");
			writer.append("<body>");
			writer.append("<p>Hello OSGi HTTP Utils World!</p>");
			writer.append("</body>");
			writer.append("</html>");
		}
		
	}
	
	@Reference(target="(http.default.service=true)")
	public void setDefaultHttpService(HttpService defaultHttpService) {
		this.defaultHttpService = defaultHttpService;
	}
	
	@Activate
	public void start() throws Exception {
		logger.info("Register Demo Servlet on default HTTP Service.");
		HttpContext httpContext = defaultHttpService.createDefaultHttpContext();
		Dictionary initparams = new Properties();
		defaultHttpService.registerServlet("/hello", new DemoServlet(), initparams , httpContext);
		logger.info("Now invoke HTTP Service under URL : http://localhost:9090/hello");
		logger.info("For details why the port is 9090 see RequestLoggerDemoJettyCustomizer.");
	}
	
	@Deactivate
	public void stop() throws Exception {
		defaultHttpService.unregister("/hello");
	}

}
