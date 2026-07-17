package vmq.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private val apiJson = Json {
    encodeDefaults = true
}

private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

@Serializable
data object HeartbeatRequestBody

@Serializable
data class PaymentNotifyRequestBody(
    val channel: Int,
    val price: Double,
)

internal data class EncodedJsonRequestBody(val json: String) {
    val bytes: ByteArray
        get() = json.toByteArray(Charsets.UTF_8)

    fun toRequestBody(): RequestBody = json.toRequestBody(jsonMediaType)
}

internal inline fun <reified T> T.encodeJsonRequestBody(): EncodedJsonRequestBody {
    return EncodedJsonRequestBody(apiJson.encodeToString(this))
}
