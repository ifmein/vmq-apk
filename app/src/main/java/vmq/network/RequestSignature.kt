package vmq.network

import java.util.UUID
import vmq.util.HashUtils

object RequestSignature {
    const val TIMESTAMP_HEADER = "X-VMQ-Timestamp"
    const val NONCE_HEADER = "X-VMQ-Nonce"
    const val SIGNATURE_HEADER = "X-VMQ-Signature"
    const val VERSION = "VMQ-HMAC-SHA256-V1"
    const val PREFIX = "v1="

    fun newNonce(): String = UUID.randomUUID().toString()

    fun buildCanonicalRequest(
        method: String,
        path: String,
        timestamp: String,
        nonce: String,
        body: ByteArray,
    ): String = listOf(VERSION, method.uppercase(), path, timestamp, nonce, HashUtils.sha256Hex(body)).joinToString("\n")

    fun sign(
        method: String,
        path: String,
        timestamp: String,
        nonce: String,
        body: ByteArray,
        key: String,
    ): String = PREFIX + HashUtils.hmacSha256Hex(
        buildCanonicalRequest(method, path, timestamp, nonce, body),
        key,
    )

    fun buildHeaders(
        method: String,
        path: String,
        timestamp: String,
        nonce: String,
        body: ByteArray,
        key: String,
    ): Map<String, String> = mapOf(
        TIMESTAMP_HEADER to timestamp,
        NONCE_HEADER to nonce,
        SIGNATURE_HEADER to sign(method, path, timestamp, nonce, body, key),
    )
}
