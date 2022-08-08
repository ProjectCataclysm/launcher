package cataclysm.launcher.utils;

import okhttp3.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Created 16 ���. 2018 �. / 21:19:05
 *
 * @author Knoblul
 */
public class HttpClientWrapper {
	private static final OkHttpClient HTTP_CLIENT;
	private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

	// тайм-аут всех подключений, в миллисекундах
	private static final Duration REQ_TIMEOUT = Duration.of(15, ChronoUnit.SECONDS);

	/**
	 * Делает GET-запрос на указанный адрес.
	 * Проверяет ответ на наличие статуса 200 OK.
	 * Если такового статуса нет в ответе, то выкидывает исключение.
	 * @param url адрес, на который обращаемся
	 * @param parser парсер ответа из {@link InputStream}
	 * @throws IOException выбрасывается, если произошла ошибка, связанная с I/O
	 */
	public static HttpResponse get(String url) throws IOException {
		Request request = new Request.Builder().url(url).get().build();
		Response response = HTTP_CLIENT.newCall(request).execute();
		int responseCode = response.code();
		if (responseCode != 200) {
			response.close();
			throw new IOException("Invalid response status code: " + responseCode);
		}

		ResponseBody body = response.body();
		if (body == null) {
			response.close();
			throw new IOException("Missing response body!");
		}

		return new HttpResponse(response, body);
	}

	/**
	 * Данный метод служит для общения с API через HTTP-POST.
	 *
	 * @param endpoint       к какому скрипту обращаемся, относительно API
	 * @param requestBuilder запрос в JSON
	 * @return ответ в JSON
	 * @throws IOException если какая-то ошибка при чтении ответа происходит, выбрасывается это исключение.
	 */
	public static JSONObject postJsonRequest(String endpoint, JSONObject requestBuilder)
			throws ApiException, IOException {
		Request request = new Request.Builder()
				.url("https://" + LauncherConstants.API_URL + "/" + endpoint)
				.post(RequestBody.create(requestBuilder.toJSONString(), MEDIA_TYPE_JSON))
				.build();
		try (Response response = HTTP_CLIENT.newCall(request).execute();
		    ResponseBody responseBody = response.body()) {
			if (responseBody == null) {
				throw new IOException("Invalid response: no body");
			}

			MediaType mediaType = responseBody.contentType();
			if (mediaType == null) {
				// выбрасываем исключение, если сервер не задал заголовок content-type
				throw new IOException("Invalid response: Content-Type header is missing");
			}

			// выбрасываем исключение, если сервер не задал заголовок content-type как application/json
			// например, если сервак просрочен или лежит, то хостинг подменяет все странички на свою заглушку
			// соотв. ответ будет совсем не то, что нам нужно
			if (!mediaType.type().equals(MEDIA_TYPE_JSON.type())
					|| !mediaType.subtype().equals(MEDIA_TYPE_JSON.subtype())) {
				throw new IOException("Invalid response: Content-Type is not json: " + mediaType);
			}

			try {
				// парсим ответ
				JSONObject node = (JSONObject) new JSONParser().parse(responseBody.charStream());

				// если сервер ответил нам ошибкой, она хранится в ноде error
				if (node.containsKey("error")) {
					throw new ApiException((String) node.get("error"));
				} else if (!node.containsKey("response")) {
					// если в данных ответа нет ноды response
					throw new IOException("Json does not contains 'response'!");
				}

				if (!(node.get("response") instanceof JSONObject)) {
					return null;
				}

				return (JSONObject) node.get("response");
			} catch (ParseException e) {
				throw new IOException("Failed to parse response '" + responseBody + "'", e);
			}
		}
	}

	public static void browse(URI uri) {
		try {
			//noinspection UnnecessaryFullyQualifiedName
			java.awt.Desktop.getDesktop().browse(uri);
		} catch (IOException e) {
			Log.err(e, "Failed to open game directory");
		}
	}

	public static void browse(String url) {
		browse(URI.create(url));
	}

	//	private static final DefaultHttpRequestRetryHandler RETRY_HANDLER = new DefaultHttpRequestRetryHandler(3, false);
//	private static final SSLContext SSL_CONTEXT;

	static {
		try {
			// обход сертификата
			SSLContext sslContext = SSLContext.getInstance("SSL");
			TrustManager[] trustManagers = { new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) {

				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) {

				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			} };
			sslContext.init(null, trustManagers, new SecureRandom());
			SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			HTTP_CLIENT = new OkHttpClient.Builder()
					.readTimeout(REQ_TIMEOUT)
					.connectTimeout(REQ_TIMEOUT)
					.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManagers[0])
					.hostnameVerifier((s, sslSession) -> true)
					.build();
		} catch (Exception e) {
			throw new RuntimeException("failed to create http client", e);
		}
	}

	public static final class HttpResponse implements Closeable {
		private final Response response;
		private final ResponseBody body;

		public HttpResponse(Response response, ResponseBody body) {
			this.response = response;
			this.body = body;
		}

		public Response getResponse() {
			return response;
		}

		public ResponseBody getBody() {
			return body;
		}

		@Override
		public void close() throws IOException {
			response.close();
			body.close();
		}
	}
}
