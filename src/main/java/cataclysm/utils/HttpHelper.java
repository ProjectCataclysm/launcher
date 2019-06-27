package cataclysm.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created 16 окт. 2018 г. / 21:19:05
 * 
 * @author Knoblul
 */
public class HttpHelper {
	public static final String WORK_SITE_URL = "project-cataclysm.ru";
	public static final String CLIENT_URL = WORK_SITE_URL + "/files/client";
	public static final String LAUNCHER_URL = WORK_SITE_URL + "/files/launcher";

	public static URL clientURL(String file) throws MalformedURLException {
		return new URL("http://" + CLIENT_URL + "/" + file);
	}
	
	public static URL launcherURL(String file) throws MalformedURLException {
		return new URL("http://" + LAUNCHER_URL + "/" + file);
	}
	
	public static void close(URLConnection connection) {
		if (connection instanceof HttpURLConnection) {
			try {
				((HttpURLConnection) connection).disconnect();
			} catch (Throwable t) {
			}
		}
	}

	public static URLConnection open(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(15000);
		return connection;
	}
	
	public static InputStream openStream(URL url) throws IOException {
		return open(url).getInputStream();
	}
}
