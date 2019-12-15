package cataclysm.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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
import java.util.Map.Entry;

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
		return new URL("https://" + CLIENT_URL + "/" + file);
	}
	
	public static URL launcherURL(String file) throws MalformedURLException {
		return new URL("https://" + LAUNCHER_URL + "/" + file);
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
	
	public static String buildPostString(Map<String, String> args) {
		StringBuilder result = new StringBuilder();
		for (Entry<String, String> entry: args.entrySet()) {
			if (result.length() > 0) {
				result.append('&');
			}

			try {
				result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			if (entry.getValue() != null) {
				result.append('=');
				try {
					result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		return result.toString();
	}
	
	public static String postRequest(String script, Map<String, String> args) {
		try {
			String content = buildPostString(args);
			URL url = new URL("https://" + script);
			
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", "" + content.getBytes().length);
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
            	out.writeBytes(content);
            	out.flush();
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8))) {
            	String ln;
            	while ((ln = reader.readLine()) != null)  {
            		response.append(ln);
            	}
            }
            
            return response.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "error: " + e.toString();
		}
	}
}
