package vmq.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiUrlBuilderTest {
    @Test
    fun `build adds http scheme when missing`() {
        val result = ApiUrlBuilder.build("example.com:8080", "api/v1/system/heartbeat?t=1&sign=abc")

        assertEquals("http://example.com:8080/api/v1/system/heartbeat?t=1&sign=abc", result)
    }

    @Test
    fun `build preserves https scheme`() {
        val result = ApiUrlBuilder.build("https://example.com", "api/v1/system/heartbeat?t=1&sign=abc")

        assertEquals("https://example.com/api/v1/system/heartbeat?t=1&sign=abc", result)
    }

    @Test
    fun `build trims trailing slashes from host`() {
        val result = ApiUrlBuilder.build("https://example.com///", "api/v1/system/heartbeat?t=1&sign=abc")

        assertEquals("https://example.com/api/v1/system/heartbeat?t=1&sign=abc", result)
    }

    @Test
    fun `build app push url keeps path`() {
        val result = ApiUrlBuilder.buildAppPushUrl(
            hostValue = "https://example.com/api",
        )

        assertEquals(
            "https://example.com/api/api/v1/payments/notify",
            result,
        )
    }
}
