package vmq.network

import vmq.data.AppConfig
import vmq.model.PaymentEvent
import vmq.util.HashUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import vmq.util.RetryConfig
import vmq.util.RetryableException
import vmq.util.retry

class PaymentPushService(
    okHttpClient: OkHttpClient = OkHttpClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val client = okHttpClient.newBuilder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun sendPayment(config: AppConfig, paymentEvent: PaymentEvent): Result<String> =
        withContext(dispatcher) {
            runCatching {
                withTimeout(30_000L) {
                    retry(RetryConfig(maxAttempts = 4, initialDelayMs = 1_000L)) {
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

                        try {
                            client.newCall(request).execute().use { response ->
                                when {
                                    response.isSuccessful -> response.body?.string().orEmpty()
                                    response.code in 500..599 ->
                                        throw RetryableException("Server error HTTP ${response.code}")
                                    else -> error("Unexpected HTTP ${response.code}")
                                }
                            }
                        } catch (error: IOException) {
                            throw RetryableException("Network error", error)
                        }
                    }
                }
            }
        }
}
