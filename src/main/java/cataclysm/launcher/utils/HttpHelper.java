package cataclysm.launcher.utils;

import cataclysm.launcher.io.HttpDownloadRequest;
import cataclysm.launcher.utils.exception.APIException;
import com.google.common.base.Charsets;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Created 16 авг. 2018 г. / 21:19:05
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
			} catch (Throwable ignored) {

			}
		}
	}

	public static HttpURLConnection createConnection(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setUseCaches(false);
		connection.setConnectTimeout(REQ_TIMEOUT);
		connection.setReadTimeout(REQ_TIMEOUT);
		try {
			return (HttpURLConnection) connection;
		} catch (ClassCastException e) {
			throw new IOException("Invalid connection type, probably url is wrong");
		}
	}

	public static InputStream openStream(URL url) throws IOException {
		return createConnection(url).getInputStream();
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
	public static Object executeJsonRequest(String script, JSONObject request) throws IOException {
		URL url = new URL("https://" + script);

		String content = Optional.ofNullable(request).map(r -> JSONObject.toJSONString(r)).orElse(null);

		HttpURLConnection connection = createConnection(url);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		connection.setRequestProperty("Content-Language", "en-US");
		connection.setDoInput(true);
		connection.setDoOutput(true);

		if (content != null) {
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(),
					StandardCharsets.UTF_8))) {
				writer.write(content);
			}
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

		Object responseObject;
		try {
			if (result.trim().isEmpty()) {
				return new JSONObject();
			}

			responseObject = new JSONParser().parse(result.trim());
		} catch (ParseException e) {
			throw new IOException(e);
		}

		if (responseObject instanceof  JSONObject) {
			JSONObject json = (JSONObject) responseObject;
			if (json.containsKey("error")) {
				throw new APIException((String) json.get("error"));
			}
		}

		return responseObject;
	}

	public static JSONObject executeJsonObjectRequest(String script, JSONObject request) throws IOException {
		try {
			return (JSONObject) executeJsonRequest(script, request);
		} catch (ClassCastException e) {
			throw new IOException(e);
		}
	}

	public static <T> List<T> executeJsonArrayRequest(String script, JSONObject request) throws IOException {
		try {
			return JsonUtil.unpackArray((JSONArray) executeJsonRequest(script, request));
		} catch (ClassCastException e) {
			throw new IOException(e);
		}
	}

	public static String getFileContents(URL url) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(openStream(url), Charsets.UTF_8))) {
			StringBuilder result = new StringBuilder();
			for (String ln = reader.readLine(); ln != null; ln = reader.readLine()) {
				result.append(ln).append("\n");
			}
			return result.toString();
		}
	}

	public static void downloadFile(HttpDownloadRequest downloadRequest) throws IOException {
		HttpURLConnection connection = createConnection(downloadRequest.getRemoteFileURL());

		long offset = downloadRequest.getRequestedOffset();
		long size = downloadRequest.getRequestedSize();
		if (offset > 0 || size > 0) {
			String range = "bytes=" + (offset > 0 ? offset : "") + "-" + (size > 0 ? size : "");
			connection.setRequestProperty("Range", range);
		}

		try (InputStream in = connection.getInputStream()) {
			Optional.ofNullable(downloadRequest.getDownloadSizeConsumer()).ifPresent(consumer ->
					consumer.accept(connection.getContentLength()));
			OpenOption[] openOptions = downloadRequest.isLocalFileAppend() ?
					new OpenOption[] { StandardOpenOption.APPEND } : new OpenOption[0];
			try (OutputStream out = Files.newOutputStream(downloadRequest.getLocalFilePath(), openOptions)) {
				long read = 0;
				byte[] buffer = new byte[8 * 1024];
				int len;
				while ((len = in.read(buffer)) > 0) {
					out.write(buffer, 0, len);
					read += len;
					if (downloadRequest.getDownloadProgressConsumer() != null) {
						downloadRequest.getDownloadProgressConsumer().accept(read);
					}
				}
			}
		} finally {
			close(connection);
		}
	}
}
