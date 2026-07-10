package vmq.util

import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object HashUtils {
    private const val TAG = "HashUtils"

    fun md5(value: String?): String {
        if (value.isNullOrEmpty()) {
            return ""
        }

        return try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val bytes = messageDigest.digest(value.toByteArray())
            buildString {
                for (currentByte in bytes) {
                    val temp = Integer.toHexString(currentByte.toInt() and 0xff)
                    if (temp.length == 1) {
                        append('0')
                    }
                    append(temp)
                }
            }
        } catch (error: NoSuchAlgorithmException) {
            Log.e(TAG, "MD5 algorithm missing", error)
            ""
        }
    }
}
