package vmq.util

import android.util.Log
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HashUtils {
    private const val TAG = "HashUtils"
    private const val HMAC_SHA256 = "HmacSHA256"

    fun sha256Hex(value: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(value).toHex()
    }

    fun hmacSha256Hex(value: String, key: String): String {
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), HMAC_SHA256)
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(secretKey)
        return mac.doFinal(value.toByteArray(Charsets.UTF_8)).toHex()
    }

    fun signGen(value: String?, key: String?): String {
        if (value.isNullOrEmpty() || key.isNullOrEmpty()) {
            return ""
        }

        return try {
            hmacSha256Hex(value, key)
        } catch (error: Exception) {
            Log.e(TAG, "HMAC-SHA256 algorithm missing", error)
            ""
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
}
