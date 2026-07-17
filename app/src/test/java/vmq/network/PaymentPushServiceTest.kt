package vmq.network

import vmq.data.AppConfig
import vmq.model.PaymentEvent
import vmq.model.PaymentType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
                nonceFactory = { "123e4567-e89b-42d3-a456-426614174000" },
            )

            val result = service.sendPayment(config, paymentEvent)
            val request = server.takeRequest()
            val channel = paymentEvent.type.code
            val body = "{\"channel\":$channel,\"price\":${paymentEvent.amount}}"

            assertTrue(result.isSuccess)
            assertEquals("PUSH_OK", result.getOrNull())
            assertEquals("/api/api/v1/payments/notify", request.path)
            assertEquals(body, request.body.readUtf8())
            assertEquals("$timestamp", request.getHeader(RequestSignature.TIMESTAMP_HEADER))
            assertEquals("123e4567-e89b-42d3-a456-426614174000", request.getHeader(RequestSignature.NONCE_HEADER))
            assertEquals(
                RequestSignature.sign(
                    method = "POST",
                    path = "/api/api/v1/payments/notify",
                    timestamp = timestamp.toString(),
                    nonce = "123e4567-e89b-42d3-a456-426614174000",
                    body = body.toByteArray(Charsets.UTF_8),
                    key = "secret",
                ),
                request.getHeader(RequestSignature.SIGNATURE_HEADER),
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
            val nonces = listOf(
                "123e4567-e89b-42d3-a456-426614174000",
                "123e4567-e89b-42d3-a456-426614174001",
            ).iterator()
            val service = PaymentPushService(
                okHttpClient = OkHttpClient(),
                dispatcher = StandardTestDispatcher(testScheduler),
                currentTimeMillis = { 123456789L },
                nonceFactory = { nonces.next() },
            )

            val result = service.sendPayment(
                config = AppConfig(host = server.url("/").toString().removeSuffix("/"), key = "secret"),
                paymentEvent = PaymentEvent(PaymentType.WECHAT, 8.88),
            )
            val first = server.takeRequest()
            val second = server.takeRequest()

            assertTrue(result.isSuccess)
            assertEquals("PUSH_OK", result.getOrNull())
            assertEquals(2, server.requestCount)
            assertNotEquals(
                first.getHeader(RequestSignature.NONCE_HEADER),
                second.getHeader(RequestSignature.NONCE_HEADER),
            )
            for (request in listOf(first, second)) {
                val body = request.body.readByteArray()
                assertEquals(
                    RequestSignature.sign(
                        method = request.method!!,
                        path = request.path!!.substringBefore("?"),
                        timestamp = request.getHeader(RequestSignature.TIMESTAMP_HEADER)!!,
                        nonce = request.getHeader(RequestSignature.NONCE_HEADER)!!,
                        body = body,
                        key = "secret",
                    ),
                    request.getHeader(RequestSignature.SIGNATURE_HEADER),
                )
            }
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `sendPayment retries on network error and succeeds on second attempt`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        server.enqueue(MockResponse().setBody("PUSH_OK"))
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
