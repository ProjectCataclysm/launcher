package cataclysm.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;

import com.google.common.base.Charsets;

/**
 * Created 16 ���. 2018 �. / 21:19:05
 * 
 * @author Knoblul
 */
public class HttpHelper {
	public static final String WORK_SITE_URL = "project-cataclysm.ru";
	public static final String CLIENT_URL = WORK_SITE_URL + "/files/client";
	public static final String LAUNCHER_URL = WORK_SITE_URL + "/files/launcher";

	public static final String SCRIPTS_DIR = WORK_SITE_URL + "/web";
	public static final String AUTH_SCRIPT = SCRIPTS_DIR+"/auth.php";
	public static final String JOINSERVER_SCRIPT = SCRIPTS_DIR+"/joinserver.php";
	public static final String CHECKSERVER_SCRIPT = SCRIPTS_DIR+"/checkserver.php";
	
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
	
	/**
	 * Encode the given string for insertion into a URL
	 */
	private static String urlEncode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String postRequest(String script, Map<String, String> args) {
		try {
			StringBuilder argsString = new StringBuilder();
			args.forEach((k, v) -> argsString.append("&").append(k).append("=").append(urlEncode(v)));
			URL url = new URL("http://" + script + "?" + argsString.substring(1));
			StringBuilder sb = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), Charsets.UTF_8))) {
				String ln;
				while ((ln = reader.readLine()) != null) {
					sb.append(ln);
				}
				return sb.toString();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "error: " + e.toString();
		}
	}
}
