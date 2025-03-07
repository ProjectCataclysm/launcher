package cataclysm.launcher.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Closeable;
import java.io.IOException;
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
	public static final Gson GSON = new GsonBuilder()
		.create();

	private static final OkHttpClient HTTP_CLIENT;
	private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

	// тайм-аут всех подключений
	private static final Duration REQ_TIMEOUT = Duration.of(15, ChronoUnit.SECONDS);

	/**
	 * Делает GET-запрос на указанный адрес.
	 * Проверяет ответ на наличие статуса 200 OK.
	 * Если такового статуса нет в ответе, то выкидывает исключение.
	 * @param url адрес, на который обращаемся
	 * @throws IOException выбрасывается, если произошла ошибка, связанная с I/O
	 */
	public static HttpGetResponse get(String url) throws IOException {
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

		return new HttpGetResponse(response, body);
	}

	/**
	 * Данный метод служит для общения с API через HTTP-POST.
	 *
	 * @param endpoint       к какому скрипту обращаемся, относительно API
	 * @param req запрос в JSON
	 * @return ответ в JSON
	 * @throws IOException если какая-то ошибка при чтении ответа происходит, выбрасывается это исключение.
	 */
	public static <T> T postJsonRequest(String endpoint, Object req, @Nullable Class<T> responseClass)
			throws ApiException, IOException {
		Request request = new Request.Builder()
				.url("https://" + LauncherConstants.API_URL + "/" + endpoint)
				.post(RequestBody.create(GSON.toJson(req), MEDIA_TYPE_JSON))
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
				JsonObject obj = GSON.fromJson(responseBody.charStream(), JsonObject.class);

				// если сервер ответил нам ошибкой, она хранится в ноде error
				if (obj.has("error")) {
					throw new ApiException(obj.get("error").getAsString());
				} else if (!obj.has("response")) {
					// если в данных ответа нет ноды response
					throw new IOException("Json does not contains 'response'!");
				}

				// парсим ответ
				return responseClass != null ? GSON.fromJson(obj.get("response"), responseClass) : null;
			} catch (JsonSyntaxException e) {
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

	public static final class HttpGetResponse implements Closeable {
		private final Response response;
		private final ResponseBody body;

		public HttpGetResponse(Response response, ResponseBody body) {
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
		public void close() {
			response.close();
			body.close();
		}
	}
}
