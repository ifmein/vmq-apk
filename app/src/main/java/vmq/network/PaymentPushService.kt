package vmq.network

import vmq.data.AppConfig
import vmq.model.PaymentEvent
import vmq.util.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PaymentPushService(
    private val okHttpClient: OkHttpClient = OkHttpClient(),
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun sendPayment(config: AppConfig, paymentEvent: PaymentEvent): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = (currentTimeMillis() / 1_000).toString()
            val channel = paymentEvent.type.code
            val sign = HashUtils.signGen("$channel${paymentEvent.amount}$timestamp", config.key)
            val requestBody = PaymentNotifyRequestBody(
                channel = channel,
                price = paymentEvent.amount,
                t = timestamp,
                sign = sign,
            ).toJsonRequestBody()
            val request = Request.Builder()
                .url(ApiUrlBuilder.buildAppPushUrl(config.host))
                .post(requestBody)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected HTTP ${response.code}" }
                response.body?.string().orEmpty()
            }
        }
    }
}
