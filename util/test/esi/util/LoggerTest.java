package esi.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.LogManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Jdk14Logger;

import org.junit.BeforeClass;
import org.junit.Test;

public class LoggerTest {

	private static Log log;

	@BeforeClass
	public static void setUpBeforeClass() {
		log = Config.getLogger();
	}

	@Test
	public void name() {
		assertTrue(log instanceof Jdk14Logger);
		assertEquals(LoggerTest.class.getName(), ((Jdk14Logger) log).getLogger().getName());
	}

	@Test
	public void output() throws InterruptedException {
		String logDirPattern = LogManager.getLogManager().getProperty("java.util.logging.FileHandler.pattern");
		String logFileName   = logDirPattern.replace("%g", "0");

		// lastModified is 0L if file does not exist
		File logFile      = new File(logFileName);
		long lastModified = logFile.lastModified();

		// Assumes that file logging is enabled at some level
		log.fatal("Test");

		Thread.sleep(1000);
		assertTrue(logFile.lastModified() != lastModified
				|| Math.abs(lastModified - new Date().getTime()) < 5000);
	}

	@Test
	public void logDirectory() throws IOException {
		String logDirPattern = LogManager.getLogManager().getProperty("java.util.logging.FileHandler.pattern");
		File   logDir        = new File(logDirPattern.substring(0, logDirPattern.lastIndexOf('/')));

		assertEquals(Config.DIR_OUT_LOG.getCanonicalPath(), logDir.getCanonicalPath());
	}

	@Test
	public void loggingProperties() {
		String indicator = LogManager.getLogManager().getProperty("logging.properties");
		assertEquals("yes", indicator);
	}

}
