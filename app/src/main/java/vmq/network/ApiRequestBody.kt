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
data class HeartbeatRequestBody(
    val t: String,
    val sign: String,
)

@Serializable
data class PaymentNotifyRequestBody(
    val channel: Int,
    val price: Double,
    val t: String,
    val sign: String,
)

internal inline fun <reified T> T.toJsonRequestBody(): RequestBody {
    return apiJson.encodeToString(this).toRequestBody(jsonMediaType)
}
