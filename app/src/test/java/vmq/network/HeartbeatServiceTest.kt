package vmq.network

import vmq.data.AppConfig
import vmq.util.HashUtils
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartbeatServiceTest {
    @Test
    fun `sendHeartbeat returns response body and requests expected url`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("OK"))
        server.start()

        try {
            val timestamp = 123456789L
            val host = server.url("/api").toString().removeSuffix("/")
            val config = AppConfig(host = host, key = "secret")
            val service = HeartbeatService(
                okHttpClient = OkHttpClient(),
                currentTimeMillis = { timestamp },
            )

            val result = service.sendHeartbeat(config)
            val request = server.takeRequest()
            val sign = HashUtils.signGen(timestamp.toString(), "secret")

            assertTrue(result.isSuccess)
            assertEquals("OK", result.getOrNull())
            assertEquals("/api/api/v1/system/heartbeat", request.path)
            assertEquals(
                "{\"t\":\"$timestamp\",\"sign\":\"$sign\"}",
                request.body.readUtf8(),
            )
            assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"))
            assertEquals("POST", request.method)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `sendHeartbeat returns failure when request cannot connect`() = runTest {
        val service = HeartbeatService(
            okHttpClient = OkHttpClient(),
            currentTimeMillis = { 123456789L },
        )

        val result = service.sendHeartbeat(
            AppConfig(host = "http://127.0.0.1:1", key = "secret"),
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `sendHeartbeat returns failure for non successful http status`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(500).setBody("ERROR"))
        server.start()

        try {
            val service = HeartbeatService(
                okHttpClient = OkHttpClient(),
                currentTimeMillis = { 123456789L },
            )

            val result = service.sendHeartbeat(
                AppConfig(host = server.url("/").toString().removeSuffix("/"), key = "secret"),
            )

            assertTrue(result.isFailure)
        } finally {
            server.shutdown()
        }
    }
}
