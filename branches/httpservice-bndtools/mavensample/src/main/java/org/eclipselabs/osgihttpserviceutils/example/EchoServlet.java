package org.eclipselabs.osgihttpserviceutils.example;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EchoServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;

	private final String message;

	public EchoServlet(String message) {
		this.message = message;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.getWriter().println(message);
	}
	
}
