package vmq.util

import android.util.Log
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HashUtils {
    private const val TAG = "HashUtils"
    private const val HMAC_SHA256 = "HmacSHA256"

    fun signGen(value: String?, key: String?): String {
        if (value.isNullOrEmpty() || key.isNullOrEmpty()) {
            return ""
        }

        return try {
            val secretKey = SecretKeySpec(key.toByteArray(), HMAC_SHA256)
            val mac = Mac.getInstance(HMAC_SHA256)
            mac.init(secretKey)
            val bytes = mac.doFinal(value.toByteArray())
            buildString {
                for (currentByte in bytes) {
                    val temp = Integer.toHexString(currentByte.toInt() and 0xff)
                    if (temp.length == 1) {
                        append('0')
                    }
                    append(temp)
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "HMAC-SHA256 algorithm missing", error)
            ""
        }
    }
}
