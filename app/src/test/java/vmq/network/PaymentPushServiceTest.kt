package vmq.network

import vmq.data.AppConfig
import vmq.model.PaymentEvent
import vmq.model.PaymentType
import vmq.util.HashUtils
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentPushServiceTest {
    @Test
    fun `sendPayment returns response body and requests expected url`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("PUSH_OK"))
        server.start()

        try {
            val timestamp = 123456789L
            val config = AppConfig(
                host = server.url("/api").toString().removeSuffix("/"),
                key = "secret",
            )
            val paymentEvent = PaymentEvent(PaymentType.ALIPAY, 12.34)
            val service = PaymentPushService(
                okHttpClient = OkHttpClient(),
                currentTimeMillis = { timestamp },
            )

            val result = service.sendPayment(config, paymentEvent)
            val request = server.takeRequest()
            val sign = HashUtils.md5("${paymentEvent.type.code}${paymentEvent.amount}${timestamp}secret")

            assertTrue(result.isSuccess)
            assertEquals("PUSH_OK", result.getOrNull())
            assertEquals(
                "/api/appPush?t=$timestamp&type=${paymentEvent.type.code}&price=${paymentEvent.amount}&sign=$sign",
                request.path,
            )
            assertEquals("GET", request.method)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `sendPayment returns failure for non successful http status`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(500).setBody("ERROR"))
        server.start()

        try {
            val service = PaymentPushService(
                okHttpClient = OkHttpClient(),
                currentTimeMillis = { 123456789L },
            )

            val result = service.sendPayment(
                config = AppConfig(host = server.url("/").toString().removeSuffix("/"), key = "secret"),
                paymentEvent = PaymentEvent(PaymentType.WECHAT, 8.88),
            )

            assertTrue(result.isFailure)
        } finally {
            server.shutdown()
        }
    }
}
