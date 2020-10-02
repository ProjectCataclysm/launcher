package cataclysm.utils;

import com.google.common.base.Charsets;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created 16 ���. 2018 �. / 21:19:05
 * @author Knoblul
 */
public class HttpHelper {
	private static final int REQ_TIMEOUT = 15000;

	public static final String WORK_SITE_URL = "project-cataclysm.ru";
	public static final String CLIENT_URL = WORK_SITE_URL + "/files/client";
	public static final String LAUNCHER_URL = WORK_SITE_URL + "/files/launcher";
	public static final String API_URL = WORK_SITE_URL + "/api";

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
		connection.setConnectTimeout(REQ_TIMEOUT);
		connection.setReadTimeout(REQ_TIMEOUT);
		return connection;
	}

	public static InputStream openStream(URL url) throws IOException {
		return open(url).getInputStream();
	}

	public static String buildPostString(Map<String, String> args) {
		StringBuilder result = new StringBuilder();
		for (Entry<String, String> entry : args.entrySet()) {
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

	@SuppressWarnings("unchecked")
	public static JSONObject postJsonRequest(String script, JSONObject request) throws IOException {
		URL url = new URL("https://" + script);

		String content = request.toJSONString();

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		connection.setRequestProperty("Content-Language", "en-US");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setReadTimeout(REQ_TIMEOUT);
		connection.setReadTimeout(REQ_TIMEOUT);

		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(),
				StandardCharsets.UTF_8))) {
			writer.write(content);
		}

		String result;
		if (connection.getResponseCode() != 200) {
			InputStream in = connection.getErrorStream();
			if (in != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8))) {
					StringBuilder response = new StringBuilder();
					String ln;
					while ((ln = reader.readLine()) != null) {
						response.append(ln);
					}
					result = response.toString();
				}
			} else {
				result = "";
			}

			if (result.isEmpty()) {
				JSONObject json = new JSONObject();
				json.put("error", connection.getResponseCode() + " " + connection.getResponseMessage());
				result = json.toString();
			}
		} else {
			InputStream in = connection.getInputStream();
			if (in != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8))) {
					StringBuilder response = new StringBuilder();
					String ln;
					while ((ln = reader.readLine()) != null) {
						response.append(ln);
					}
					result = response.toString();
				}
			} else {
				result = "";
			}
		}

		JSONObject json;
		try {
			if (result.trim().isEmpty()) {
				return new JSONObject();
			}
			json = (JSONObject) new JSONParser().parse(result.trim());
		} catch (ParseException e) {
			throw new IOException(e);
		}

		if (json.containsKey("error")) {
			String error = (String) json.get("error");
			throw new IOException(error);
		}

		return json;
	}
}
