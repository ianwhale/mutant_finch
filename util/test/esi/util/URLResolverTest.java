package esi.util;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

public class URLResolverTest {

	@Test
	public void testURLResolver() {
		new URLResolver();
	}

	@Test
	public void resolve() {
		URLResolver resolver = new URLResolver();
		URL         relative = resolver.resolve("."); 

		assertEquals("file", relative.getProtocol());
		assertEquals(resolver.url, relative);
	}

	@Test
	public void resolveDir() {
		URLResolver resolver = new URLResolver();
		URLResolver relative = resolver.resolveDir("."); 

		assertEquals(resolver.url, relative.url);
	}

	@Test
	public void resolveFile() {
		URL file = new URLResolver().resolve("file:/etc/passwd"); 
		assertEquals("file:/etc/passwd", file.toString());
	}
	
	@Test
	public void resolveAbsolute() {
		URL file = new URLResolver().resolve("/root/file"); 
		assertEquals("file:/root/file", file.toString());
	}
	
	@Test
	public void resolveCurrent() {
		URL file = new URLResolver().resolveDir("/root/dir").resolve("."); 
		assertEquals("file:/root/dir/", file.toString());
	}
	
	@Test
	public void resolveCurrentDir() {
		URLResolver file = new URLResolver().resolveDir("/root/dir").resolveDir("."); 
		assertEquals("file:/root/dir/", file.url.toString());
	}
	
	@Test
	public void resolveHTTP() {
		URLResolver url = new URLResolver().resolveDir("http://localhost/ab"); 
		assertEquals("http://localhost/ab/", url.url.toString());
	}
	
	@Test
	public void resolveJar() {
		URL url = new URLResolver().resolve("jar:file:/root/library.jar!/obj/Run.class");
		assertEquals("jar", url.getProtocol());
	}

	@Test
	public void resolveJarRelative() {
		URL url = new URLResolver().resolveDir("jar:file:/root/library.jar!/obj").resolve("Run.class");
		assertEquals("jar", url.getProtocol());
	}

	@Test(expected = IllegalArgumentException.class)
	public void resolveError() {
		new URLResolver().resolve("fake:1234567");
	}

}
