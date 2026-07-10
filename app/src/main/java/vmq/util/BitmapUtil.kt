package vmq.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.IOException
import java.io.InputStream

object BitmapUtil {
    @JvmStatic
    fun decodeUri(context: Context, uri: Uri?, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (uri == null) {
            return null
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        readBitmapScale(context, uri, options)

        var scale = 1
        while ((options.outWidth / scale > maxWidth && options.outWidth / scale > maxWidth * 1.4) ||
            (options.outHeight / scale > maxHeight && options.outHeight / scale > maxHeight * 1.4)
        ) {
            scale++
        }

        options.inSampleSize = scale
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        return try {
            readBitmapData(context, uri, options)
        } catch (throwable: Throwable) {
            Log.e("BitmapUtil", "Unable to decode bitmap: $uri", throwable)
            null
        }
    }

    private fun readBitmapScale(context: Context, uri: Uri, options: BitmapFactory.Options) {
        val scheme = uri.scheme
        if (scheme == ContentResolver.SCHEME_CONTENT || scheme == ContentResolver.SCHEME_FILE) {
            var stream: InputStream? = null
            try {
                stream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(stream, null, options)
            } catch (exception: Exception) {
                Log.w("readBitmapScale", "Unable to open content: $uri", exception)
            } finally {
                closeStream("readBitmapScale", uri, stream)
            }
            return
        }

        Log.e("readBitmapScale", "Unsupported content: $uri")
    }

    private fun readBitmapData(
        context: Context,
        uri: Uri,
        options: BitmapFactory.Options,
    ): Bitmap? {
        val scheme = uri.scheme
        if (scheme == ContentResolver.SCHEME_CONTENT || scheme == ContentResolver.SCHEME_FILE) {
            var stream: InputStream? = null
            return try {
                stream = context.contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(stream, null, options)
            } catch (exception: Exception) {
                Log.e("readBitmapData", "Unable to open content: $uri", exception)
                null
            } finally {
                closeStream("readBitmapData", uri, stream)
            }
        }

        Log.e("readBitmapData", "Unsupported content: $uri")
        return null
    }

    private fun closeStream(tag: String, uri: Uri, stream: InputStream?) {
        if (stream == null) {
            return
        }

        try {
            stream.close()
        } catch (exception: IOException) {
            Log.e(tag, "Unable to close content: $uri", exception)
        }
    }
}
