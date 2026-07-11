package vmq.ui.foreground

import android.content.Context
import android.content.Intent
import android.util.Log

object ForegroundServiceController {
    private const val TAG = "ForegroundServiceController"

    fun start(context: Context) {
        Log.d(TAG, "Starting PersistentForegroundService")
        val intent = Intent(context, PersistentForegroundService::class.java)
        context.startForegroundService(intent)
    }

    fun stop(context: Context) {
        Log.d(TAG, "Stopping PersistentForegroundService")
        val intent = Intent(context, PersistentForegroundService::class.java)
        context.stopService(intent)
    }
}
