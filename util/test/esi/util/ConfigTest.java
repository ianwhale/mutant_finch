package esi.util;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.junit.Test;

public class ConfigTest {

	@Test
	public void properties() {
		Properties esiProps = Config.esiProperties;
		assertTrue(esiProps.size() > 0);
	}

	@Test
	public void testDirectories() {
		testWritableDir(Config.DIR_OUT_LOG);
		testWritableDir(Config.DIR_OUT_ECJ);
		testWritableDir(Config.DIR_OUT_TESTS);
	}

	@Test
	public void testURLs() throws URISyntaxException {
		testReadableDir(Config.DIR_IN_CONF.url);
		testReadableDir(Config.DIR_IN_TESTS.url);
	}

	private void testWritableDir(File dir) {
		assertTrue(dir.exists());
		assertTrue(dir.isDirectory());
		assertTrue(dir.canWrite());
	}

	private void testReadableDir(URL dir) throws URISyntaxException {
		if (dir.getProtocol().equals("file")) {
			File testDirFile = new File(dir.toURI());

			assertTrue(testDirFile.exists());
			assertTrue(testDirFile.isDirectory());
			assertTrue(testDirFile.canRead());
		}
	}

}
