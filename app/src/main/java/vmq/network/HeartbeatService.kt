package vmq.network

import vmq.data.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class HeartbeatService(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val nonceFactory: () -> String = RequestSignature::newNonce,
) {
    suspend fun sendHeartbeat(config: AppConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = (currentTimeMillis() / 1_000).toString()
            val nonce = nonceFactory()
            val encodedBody = HeartbeatRequestBody.encodeJsonRequestBody()
            val url = ApiUrlBuilder.buildHeartBeatUrl(config.host)
            val headers = RequestSignature.buildHeaders(
                method = "POST",
                path = url.toHttpUrl().encodedPath,
                timestamp = timestamp,
                nonce = nonce,
                body = encodedBody.bytes,
                key = config.key,
            )
            val request = Request.Builder()
                .url(url)
                .post(encodedBody.toRequestBody())
                .apply { headers.forEach { (name, value) -> header(name, value) } }
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected HTTP ${response.code}" }
                response.body?.string().orEmpty()
            }
        }
    }
}
