package ru.cataclysm.helpers

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.cataclysm.services.Log
import ru.cataclysm.services.account.ResponseResult
import java.awt.Desktop
import java.io.Closeable
import java.io.IOException
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.net.ssl.*

/**
 * Created 16 ���. 2018 �. / 21:19:05
 *
 * @author Knoblul
 */
object RequestHelper {
    private val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()
    // тайм-аут всех подключений, в миллисекундах
    private val timeout: Duration = Duration.of(15, ChronoUnit.SECONDS)
    private val client: OkHttpClient = initClient()


    /**
     * Делает GET-запрос на указанный адрес.
     * Проверяет ответ на наличие статуса 200 OK.
     * Если такового статуса нет в ответе, то выкидывает исключение.
     * @param url адрес, на который обращаемся
     * @throws IOException выбрасывается, если произошла ошибка, связанная с I/O
     */
    fun get(url: String): HttpResponse {
        val request = Request.Builder().url(url).get().build()
        val response: Response = client.newCall(request).execute()
        val responseCode: Int = response.code
        if (responseCode != 200) {
            response.close()
            throw IOException("Invalid response status code: $responseCode")
        }

        val body: ResponseBody? = response.body
        if (body == null) {
            response.close()
            throw IOException("Missing response body!")
        }

        return HttpResponse(response, body)
    }

    /**
     * Данный метод служит для общения с API через HTTP-POST.
     *
     * @param endpoint       к какому скрипту обращаемся, относительно API
     * @param json запрос в JSON
     * @return ответ в JSON
     * @throws IOException если какая-то ошибка при чтении ответа происходит, выбрасывается это исключение.
     */
    fun post(endpoint: String, json: String): String {
        val request = Request.Builder()
            .url("https://" + Constants.API_URL + "/" + endpoint).post(json.toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            response.body.use { responseBody ->
                responseBody ?: throw IOException("Invalid response: no body")
                // выбрасываем исключение, если сервер не задал заголовок content-type
                val mediaType: MediaType = responseBody.contentType() ?:
                    throw IOException("Invalid response: Content-Type header is missing")

                // выбрасываем исключение, если сервер не задал заголовок content-type как application/json
                // например, если сервак просрочен или лежит, то хостинг подменяет все странички на свою заглушку
                // соотв. ответ будет совсем не то, что нам нужно
                if (mediaType.type != this.mediaType.type || mediaType.subtype != this.mediaType.subtype) {
                    throw IOException("Invalid response: Content-Type is not json: $mediaType")
                }
                return responseBody.string()
            }
        }
    }

    inline fun <reified T : Any> parseResponse(json: String): T {
        return when (val result = ResponseResult.fromJson<T>(json)) {
            is ResponseResult.Error -> throw ApiException(result.error)
            is ResponseResult.Success<T> -> result.data
        }
    }

    fun browse(uri: URI) {
        try {
            Desktop.getDesktop().browse(uri)
        } catch (e: IOException) {
            Log.err(e, "Failed to open game directory")
        }
    }

    fun browse(url: String) {
        browse(URI.create(url))
    }

    private fun initClient(): OkHttpClient {
        try {
            // обход сертификата
            val sslContext = SSLContext.getInstance("SSL")
            val trustManagers = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            })
            sslContext.init(null, trustManagers, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder().readTimeout(timeout).connectTimeout(timeout)
                .sslSocketFactory(sslSocketFactory, (trustManagers[0] as X509TrustManager))
                .hostnameVerifier { s: String?, sslSession: SSLSession? -> true }.build()
        } catch (e: Exception) {
            throw java.lang.RuntimeException("failed to create http client", e)
        }
    }
}

class HttpResponse(val response: Response, val body: ResponseBody) : Closeable {
    override fun close() {
        response.close()
        body.close()
    }
}

/**
 * <br></br><br></br>ProjectCataclysm
 * <br></br>Created: 07.08.2022 9:55
 *
 * @author Knoblul
 */
class ApiException(message: String) : RuntimeException(message) {
    override fun getLocalizedMessage(): String {
        return when (message) {
            "api.error.unavailable" -> "Сервис временно недоступен"
            "api.error.internal" -> "Внутренняя ошибка сервера"
            "api.error.invalidSession" -> "Сессия устарела, повторите вход"
            "api.error.clientAuth.invalidCredentials" -> "Неверно введен логин или пароль"
            "api.error.clientAuth.limitReached" ->
                "Превышено максимальное число допустимых попыток. Попробуйте через 5 минут."

            "api.error.clientBanned" -> "Вы забанены :/"
            else -> "Неизвестная ошибка ($message)"
        }
    }
}