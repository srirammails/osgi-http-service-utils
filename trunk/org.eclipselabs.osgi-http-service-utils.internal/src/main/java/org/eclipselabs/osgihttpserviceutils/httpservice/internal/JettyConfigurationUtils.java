package org.eclipselabs.osgihttpserviceutils.httpservice.internal;

import java.io.File;

import org.eclipselabs.osgihttpserviceutils.httpservice.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyConfigurationUtils {
	
	private static final Logger LOG = LoggerFactory
			.getLogger(HttpServerManager.class);

	static boolean existsJettyXmlConfiguration(HttpServer server) {
		final String method = "getConfigurationFile() : ";
		String serverName = server.getSymbolicName();
		String serverConfigurationDir = System
				.getProperty("jetty.server.configuration.directory");
		if (serverConfigurationDir == null)
			return false;
		File serverConfigurationDirFolder = new File(serverConfigurationDir);
		if (!serverConfigurationDirFolder.exists()) {
			LOG.debug(
					method
							+ "The configuration directory {} for the jetty server {} does not exists!",
					serverConfigurationDir, serverName);
			return false;
		}
		File jettyServerXmlConfiguration = new File(
				serverConfigurationDirFolder, serverName + "-jetty.xml");
		if (!jettyServerXmlConfiguration.exists()) {
			LOG.debug(
					method
							+ "The jetty server XML configuration file {} does not exists!",
					jettyServerXmlConfiguration.getAbsolutePath());
			return false;
		}
		return true;
	}

	static File getConfigurationFile(String serverName) {
		final String method = "getConfigurationFile() : ";
		String serverConfigurationDir = System
				.getProperty("jetty.server.configuration.directory");
		if (serverConfigurationDir == null) {
			LOG.error(method
					+ "The system property jetty.server.configuration.directory is not set.");
			throw new HttpServiceInternalException(
					"The system property jetty.server.configuration.directory is not set.");
		}
		File serverConfigurationDirFolder = new File(serverConfigurationDir);
		if (!serverConfigurationDirFolder.exists()) {
			LOG.error(
					method
							+ "The configuration directory {} for the jetty server {} does not exists!",
					serverConfigurationDir, serverName);
			throw new HttpServiceInternalException(
					"The configuration directory for the jetty server does not exists!");
		}
		File jettyServerXmlConfiguration = new File(
				serverConfigurationDirFolder, serverName + "-jetty.xml");
		if (!jettyServerXmlConfiguration.exists()) {
			LOG.error(
					method
							+ "The jetty server XML configuration file {} does not exists!",
					jettyServerXmlConfiguration.getAbsolutePath());
			throw new HttpServiceInternalException(
					"The jetty server configuration does not exists");
		}
		return jettyServerXmlConfiguration;
	}

}
