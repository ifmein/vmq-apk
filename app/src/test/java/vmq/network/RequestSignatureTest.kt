package vmq.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import vmq.util.HashUtils

class RequestSignatureTest {
    @Test
    fun `request signature matches cross language fixed vector`() {
        val body = "{\"client\":\"android\"}".toByteArray(Charsets.UTF_8)
        val canonicalRequest = """
            VMQ-HMAC-SHA256-V1
            POST
            /api/v1/system/heartbeat
            1784194689
            123e4567-e89b-42d3-a456-426614174000
            e7434a3be5536e150ea0df851036c6ca1c0eab09f6c62153409b9a83402c9f69
        """.trimIndent()

        assertEquals(
            "e7434a3be5536e150ea0df851036c6ca1c0eab09f6c62153409b9a83402c9f69",
            HashUtils.sha256Hex(body),
        )
        assertEquals(
            canonicalRequest,
            RequestSignature.buildCanonicalRequest(
                method = "POST",
                path = "/api/v1/system/heartbeat",
                timestamp = "1784194689",
                nonce = "123e4567-e89b-42d3-a456-426614174000",
                body = body,
            ),
        )
        assertEquals(
            "v1=7b6ba572f9c836e36bac9aaa30af001eb3053c5db2bd2b0128128a9c0cc4f247",
            RequestSignature.sign(
                method = "POST",
                path = "/api/v1/system/heartbeat",
                timestamp = "1784194689",
                nonce = "123e4567-e89b-42d3-a456-426614174000",
                body = body,
                key = "test_secret_key_123",
            ),
        )
    }

    @Test
    fun `nonce factory creates distinct UUID version 4 values`() {
        val first = RequestSignature.newNonce()
        val second = RequestSignature.newNonce()

        assertTrue(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$").matches(first))
        assertNotEquals(first, second)
    }
}
