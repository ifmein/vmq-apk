package vmq.ui.foreground

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

object ForegroundServiceController {
    private const val TAG = "ForegroundServiceController"

    fun start(context: Context) {
        Log.d(TAG, "Starting PersistentForegroundService")
        val intent = Intent(context, PersistentForegroundService::class.java).apply {
            action = PersistentForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun refresh(context: Context) {
        Log.d(TAG, "Refreshing PersistentForegroundService notification")
        val intent = Intent(context, PersistentForegroundService::class.java).apply {
            action = PersistentForegroundService.ACTION_REFRESH
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        Log.d(TAG, "Stopping PersistentForegroundService")
        val intent = Intent(context, PersistentForegroundService::class.java)
        context.stopService(intent)
    }
}
