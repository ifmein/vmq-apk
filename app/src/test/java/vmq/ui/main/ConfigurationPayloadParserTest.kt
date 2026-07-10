package vmq.parser

import vmq.data.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfigurationPayloadParserTest {
    @Test
    fun `parse returns config for host and key`() {
        val result = ConfigurationPayloadParser.parse("example.com:8080/secret")

        assertEquals(AppConfig(host = "example.com:8080", key = "secret"), result)
    }

    @Test
    fun `parse keeps nested path in host`() {
        val result = ConfigurationPayloadParser.parse("https://example.com/api/secret")

        assertEquals(AppConfig(host = "https://example.com/api", key = "secret"), result)
    }

    @Test
    fun `parse trims whitespace and trailing slash before key segment`() {
        val result = ConfigurationPayloadParser.parse("  https://example.com///secret  ")

        assertEquals(AppConfig(host = "https://example.com", key = "secret"), result)
    }

    @Test
    fun `parse returns null for blank payload`() {
        val result = ConfigurationPayloadParser.parse("   ")

        assertNull(result)
    }

    @Test
    fun `parse returns null when key is missing`() {
        val result = ConfigurationPayloadParser.parse("https://example.com")

        assertNull(result)
    }

    @Test
    fun `parse returns null when payload ends with separator`() {
        val result = ConfigurationPayloadParser.parse("https://example.com/secret/")

        assertNull(result)
    }
}
