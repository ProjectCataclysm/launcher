package cataclysm.launcher.utils;

import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * Created 16 ���. 2018 �. / 21:19:05
 *
 * @author Knoblul
 */
public class HttpHelper {
	private static final CloseableHttpClient HTTP_CLIENT;

	// тайм-аут всех подключений, в миллисекундах
	private static final int REQ_TIMEOUT = 15000;

	public static final String WORK_SITE_URL = "project-cataclysm.ru";
	public static final String CLIENT_URL = "files." + WORK_SITE_URL + "/files/client";
	public static final String LAUNCHER_URL = "files." + WORK_SITE_URL + "/files/launcher";
	public static final String API_URL = WORK_SITE_URL + "/api";

	public static CloseableHttpResponse get(String url) throws IOException {
		HttpGet req = new HttpGet(url);
		// добавляем таймауты к запросу
		req.setConfig(RequestConfig.custom()
				.setConnectionRequestTimeout(REQ_TIMEOUT)
				.setConnectTimeout(REQ_TIMEOUT)
				.setSocketTimeout(REQ_TIMEOUT)
				.build());

		return HTTP_CLIENT.execute(req);
	}

	/**
	 * Данный метод служит для общения с API через HTTP-POST.
	 *
	 * @param endpoint       к какому скрипту обращаемся, относительно API
	 * @param requestBuilder запрос в JSON
	 * @return ответ в JSON
	 * @throws IOException если какая-то ошибка при чтении ответа происходит, выбрасывается это исключение.
	 */
	public static JSONObject postJsonRequest(String endpoint, JSONObject requestBuilder) throws IOException {
		HttpPost req = new HttpPost("https://" + API_URL + "/" + endpoint);
		// добавляем таймауты к запросу
		req.setConfig(RequestConfig.custom()
				.setConnectionRequestTimeout(REQ_TIMEOUT)
				.setConnectTimeout(REQ_TIMEOUT)
				.setSocketTimeout(REQ_TIMEOUT)
				.build());
		req.setEntity(new StringEntity(requestBuilder.toJSONString())); // Задаем тело json-запроса
		req.setHeader("Content-Type", "application/json; charset=utf-8"); // обязательный заголовок

		try (CloseableHttpResponse response = HTTP_CLIENT.execute(req)) {
			Header contentType = response.getEntity().getContentType();
			if (contentType == null) {
				// выбрасываем исключение, если сервер не задал заголовок content-type
				throw new IOException("Invalid response: Content-Type header is missing");
			}

			// выбрасываем исключение, если сервер не задал заголовок content-type как application/json
			// например, если сервак просрочен или лежит, то хостинг подменяет все странички на свою заглушку
			// соотв. ответ будет совсем не то, что нам нужно
			if (!contentType.getValue().startsWith("application/json")) {
				throw new IOException("Invalid response: Content-Type is not json: " + contentType.getValue());
			}

			// парсим ответ
			String responseBody = EntityUtils.toString(response.getEntity());
			try {
				JSONObject node = (JSONObject) new JSONParser().parse(responseBody);

				// если сервер ответил нам ошибкой, она хранится в ноде error
				if (node.containsKey("error")) {
					throw new IOException((String) node.get("error"));
				} else if (!node.containsKey("response")) {
					// если в данных ответа нет ноды response
					throw new IOException("Json does not contains 'response'!");
				}

				return (JSONObject) node.get("response");
			} catch (ParseException e) {
				throw new IOException("Failed to parse response '" + responseBody + "'", e);
			}
		}
	}

	static {
		try {
			// обход сертификата
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {

				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {

				}
			} }, new SecureRandom());

			HTTP_CLIENT = HttpClients.custom()
					.setSSLContext(sslContext)
					.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
					.build();
		} catch (Exception e) {
			throw new RuntimeException("failed to create http client", e);
		}
	}
}
