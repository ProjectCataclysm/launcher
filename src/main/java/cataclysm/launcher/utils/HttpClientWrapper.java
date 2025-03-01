import java.io.IOException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;

public class HttpClientWrapper {
    private static final OkHttpClient client = new OkHttpClient();

    public static HttpResponse get(String url) throws IOException {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new ApiException("HTTP " + response.code() + ": " + response.message());
            }
            return new HttpResponse(response);
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                throw new NetworkException("Нет подключения к интернету", e);
            } else if (e instanceof SocketTimeoutException) {
                throw new NetworkException("Превышено время ожидания ответа от сервера", e);
            }
            throw e;
        }
    }

    public static class NetworkException extends IOException {
        public NetworkException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 