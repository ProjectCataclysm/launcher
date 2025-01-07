package ru.cataclysm.services.account

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlin.Any
import kotlin.Int
import kotlin.Nothing
import kotlin.OptIn
import kotlin.String

@Serializable(with = DataSerializer::class)
sealed class ResponseResult<out T : Any> {
    data class Success<out T : Any>(val data: T) : ResponseResult<T>()
    data class Error(val error: String, var displayable: Boolean) : ResponseResult<Nothing>()

    fun toJson() = Json.encodeToString(this)

    companion object {
        inline fun <reified T : Any> fromJson(json: String): ResponseResult<T> {
            return Json.decodeFromString<ResponseResult<T>>(json)
        }
    }
}

class DataSerializer<T : Any>(
    tSerializer: KSerializer<T>
) : KSerializer<ResponseResult<T>> {
    @Serializable
    @SerialName("ResponseResult")
    data class DataResultSurrogate<T : Any> (
        // The annotation is not necessary, but it avoids serializing "data = null"
        // for "Error" results.
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val response: T? = null,
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val error: String? = null,
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val displayable: Boolean? = null,
    )

    private val surrogateSerializer = DataResultSurrogate.serializer(tSerializer)

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun deserialize(decoder: Decoder): ResponseResult<T> {
        val surrogate = surrogateSerializer.deserialize(decoder)
        return if (surrogate.error == null) {
            if (surrogate.response != null)
                ResponseResult.Success(surrogate.response)
            else
                throw SerializationException("Missing data for successful result")
        } else ResponseResult.Error(surrogate.error, surrogate.displayable?: false )
    }

    override fun serialize(encoder: Encoder, value: ResponseResult<T>) {
        val surrogate = when (value) {
            is ResponseResult.Error -> DataResultSurrogate(error = value.error)
            is ResponseResult.Success -> DataResultSurrogate(response = value.data)
        }
        surrogateSerializer.serialize(encoder, surrogate)
    }
}

@Serializable
data class Session(val user: User, val accessToken: String) {
    fun toJson() = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Session>(json)
    }
}

@Serializable
data class User(val profile: Profile, val balance: Int, val tickets: List<Ticket>)

@Serializable
data class Profile(val uuid: String, val email: String, val username: String)

@Serializable
data class Ticket(val type: String, val name: String)

@Serializable
data class Token(val accessToken: String) {
    fun toJson() = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Token>(json)
    }
}

@Serializable
data class Auth(val login: String, val password: String) {
    fun toJson() = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String) = Json.decodeFromString<Auth>(json)
    }
}