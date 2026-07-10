package vmq.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiUrlBuilderTest {
    @Test
    fun `build adds http scheme when missing`() {
        val result = ApiUrlBuilder.build("example.com:8080", "appHeart?t=1&sign=abc")

        assertEquals("http://example.com:8080/appHeart?t=1&sign=abc", result)
    }

    @Test
    fun `build preserves https scheme`() {
        val result = ApiUrlBuilder.build("https://example.com", "appHeart?t=1&sign=abc")

        assertEquals("https://example.com/appHeart?t=1&sign=abc", result)
    }

    @Test
    fun `build trims trailing slashes from host`() {
        val result = ApiUrlBuilder.build("https://example.com///", "appHeart?t=1&sign=abc")

        assertEquals("https://example.com/appHeart?t=1&sign=abc", result)
    }

    @Test
    fun `build app push url keeps path and query`() {
        val result = ApiUrlBuilder.buildAppPushUrl(
            hostValue = "https://example.com/api",
            timestamp = "123",
            type = 2,
            price = 12.34,
            sign = "xyz",
        )

        assertEquals(
            "https://example.com/api/appPush?t=123&type=2&price=12.34&sign=xyz",
            result,
        )
    }
}
