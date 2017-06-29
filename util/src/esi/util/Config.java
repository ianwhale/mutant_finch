package esi.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Config {

	public static final Properties	esiProperties;

	// Default properties that can be overridden by JVM parameters
	private static final String		DIR_IN_BASE_DEFAULT   = ".";
	private static final String		DIR_IN_CONFIG_DEFAULT = "../conf";

	// Resources accessed as URLs
	public static final URLResolver	DIR_IN_CONF;		// Config URL (useful as parent URL)
	public static final URLResolver	DIR_IN_TESTS;	    // Tests read URL

	// Directories (not URLs)
	public static final File		DIR_OUT_LOG;		// Log write directory
	public static File		DIR_OUT_ECJ;		// ECJ write directory
	public static final File		DIR_OUT_TESTS;		// Tests write directory

	static {
		esiProperties = new Properties();

		URLResolver baseInDir = new URLResolver().resolveDir(System.getProperty("dir.in.base",   DIR_IN_BASE_DEFAULT));
		URLResolver configDir = baseInDir        .resolveDir(System.getProperty("dir.in.config", DIR_IN_CONFIG_DEFAULT));

		URL esiPropsURL     = configDir.resolve("esi.properties");
		URL loggingPropsURL = configDir.resolve("logging.properties");

		try {
			// Load logger properties if it's the first time
			LogManager logMan = LogManager.getLogManager();
			if (logMan.getProperty("logging.properties") == null) {
				logMan.readConfiguration(new BufferedInputStream(loggingPropsURL.openStream()));
				getLogger().info("Logger configured: " + loggingPropsURL);
			}

			// Load application properties after logger
			esiProperties.load(new BufferedInputStream(esiPropsURL.openStream()));
			getLogger().info("Loaded properties: " + esiPropsURL);
		} catch (IOException e) {
			throw new Error("Unable to load properties file", e);
		}

		DIR_IN_CONF		= configDir;
		DIR_IN_TESTS    = baseInDir.resolveDir(esiProperties.getProperty("dir.in.tests").toString());

		File baseOutDir = new File(            esiProperties.getProperty("dir.out.base") .toString());
		DIR_OUT_LOG     = new File(baseOutDir, esiProperties.getProperty("dir.out.log")  .toString());
		DIR_OUT_ECJ     = new File(baseOutDir, esiProperties.getProperty("dir.out.ecj")  .toString());
		DIR_OUT_TESTS   = new File(baseOutDir, esiProperties.getProperty("dir.out.tests").toString());
	}

	/**
	 * Classes should use this method instead of {@link Logger#getLogger(String)},
	 * so that logging initialization is ensured.
	 *
	 * @return class-specific logger
	 */
	public static Log getLogger() {
		StackTraceElement frame = Thread.currentThread().getStackTrace()[2];
		return LogFactory.getLog(frame.getClassName());
	}

}
