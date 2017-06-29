package esi.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class URLResolver {
	
	// Package access for testing purposes
	final URL url;

	public URLResolver() {
		try {
			url = new File(System.getProperty("user.dir", ".")).toURI().toURL();
		} catch (MalformedURLException e) {
			throw new Error("Unexpected error converting user.dir to URL", e);
		}
	}
	
	private URLResolver(URL uri) {
		this.url = uri;
	}
	
	public URL resolve(String url) {
		try {
			return new URLResolver(new URL(this.url, url)).url;
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Can't resolve URL", e);
		}
	}

	public URLResolver resolveDir(String url) {
		try {
			return new URLResolver(new URL(this.url, url + "/"));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Can't resolve directory URL", e);
		}
	}

}
