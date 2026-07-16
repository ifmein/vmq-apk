package vmq.network

import vmq.data.AppConfig
import vmq.model.PaymentEvent
import vmq.model.PaymentType
import vmq.util.HashUtils
import kotlinx.coroutines.test.StandardTestDispatcher
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
            val timestampMillis = 1_784_194_689_610L
            val timestamp = timestampMillis / 1_000
            val config = AppConfig(
                host = server.url("/api").toString().removeSuffix("/"),
                key = "secret",
            )
            val paymentEvent = PaymentEvent(PaymentType.ALIPAY, 12.34)
            val service = PaymentPushService(
                okHttpClient = OkHttpClient(),
                dispatcher = StandardTestDispatcher(testScheduler),
                currentTimeMillis = { timestampMillis },
            )

            val result = service.sendPayment(config, paymentEvent)
            val request = server.takeRequest()
            val channel = paymentEvent.type.code
            val sign = HashUtils.signGen("$channel${paymentEvent.amount}$timestamp", "secret")

            assertTrue(result.isSuccess)
            assertEquals("PUSH_OK", result.getOrNull())
            assertEquals("/api/api/v1/payments/notify", request.path)
            assertEquals(
                "{\"channel\":$channel,\"price\":${paymentEvent.amount},\"t\":\"$timestamp\",\"sign\":\"$sign\"}",
                request.body.readUtf8(),
            )
            assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"))
            assertEquals("POST", request.method)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `sendPayment retries on 500 and succeeds on second attempt`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(500).setBody("ERROR"))
        server.enqueue(MockResponse().setBody("PUSH_OK"))
        server.start()

        try {
            val service = PaymentPushService(
                okHttpClient = OkHttpClient(),
                dispatcher = StandardTestDispatcher(testScheduler),
                currentTimeMillis = { 123456789L },
            )

            val result = service.sendPayment(
                config = AppConfig(host = server.url("/").toString().removeSuffix("/"), key = "secret"),
                paymentEvent = PaymentEvent(PaymentType.WECHAT, 8.88),
            )

            assertTrue(result.isSuccess)
            assertEquals("PUSH_OK", result.getOrNull())
            assertEquals(2, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `sendPayment does not retry on 400`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(400).setBody("INVALID"))
        server.start()

        try {
            val service = PaymentPushService(
                okHttpClient = OkHttpClient(),
                dispatcher = StandardTestDispatcher(testScheduler),
            )

            val result = service.sendPayment(
                config = AppConfig(host = server.url("/").toString().removeSuffix("/"), key = "secret"),
                paymentEvent = PaymentEvent(PaymentType.WECHAT, 8.88),
            )

            assertTrue(result.isFailure)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `sendPayment exhausts retries on persistent 500`() = runTest {
        val server = MockWebServer()
        repeat(4) { server.enqueue(MockResponse().setResponseCode(500)) }
        server.start()

        try {
            val service = PaymentPushService(
                okHttpClient = OkHttpClient(),
                dispatcher = StandardTestDispatcher(testScheduler),
            )

            val result = service.sendPayment(
                config = AppConfig(host = server.url("/").toString().removeSuffix("/"), key = "secret"),
                paymentEvent = PaymentEvent(PaymentType.WECHAT, 8.88),
            )

            assertTrue(result.isFailure)
            assertEquals(4, server.requestCount)
        } finally {
            server.shutdown()
        }
    }
}
