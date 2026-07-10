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
            val timestamp = currentTimeMillis().toString()
            val sign = HashUtils.md5(paymentEvent.type.code.toString() + paymentEvent.amount + timestamp + config.key)
            val request = Request.Builder()
                .url(
                    ApiUrlBuilder.buildAppPushUrl(
                        hostValue = config.host,
                        timestamp = timestamp,
                        type = paymentEvent.type.code,
                        price = paymentEvent.amount,
                        sign = sign,
                    ),
                )
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected HTTP ${response.code}" }
                response.body?.string().orEmpty()
            }
        }
    }
}
