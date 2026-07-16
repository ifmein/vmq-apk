package vmq.network

import vmq.data.AppConfig
import vmq.util.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HeartbeatService(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun sendHeartbeat(config: AppConfig): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = (currentTimeMillis() / 1_000).toString()
            val sign = HashUtils.signGen(timestamp, config.key)
            val requestBody = HeartbeatRequestBody(
                t = timestamp,
                sign = sign,
            ).toJsonRequestBody()
            val request = Request.Builder()
                .url(ApiUrlBuilder.buildHeartBeatUrl(config.host))
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected HTTP ${response.code}" }
                response.body?.string().orEmpty()
            }
        }
    }
}
