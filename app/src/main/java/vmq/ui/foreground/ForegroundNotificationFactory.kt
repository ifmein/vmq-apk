package vmq.ui.foreground

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.vone.qrcode.R
import vmq.data.ListenerStatusStore
import vmq.ui.main.MainActivity
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ForegroundNotificationFactory(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val statusStore: ListenerStatusStore,
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun createOrUpdateChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.foreground_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.foreground_channel_description)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(): android.app.Notification {
        val contentText = buildContentText()
        val pendingIntent = createPendingIntent()

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.foreground_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun buildContentText(): String {
        val listenerStatus = if (statusStore.isListenerConnected) {
            context.getString(R.string.foreground_status_connected)
        } else {
            context.getString(R.string.foreground_status_disconnected)
        }

        val configStatus = if (statusStore.lastHeartbeatSuccessAt != null || statusStore.lastHeartbeatErrorMessage != null) {
            context.getString(R.string.foreground_status_configured)
        } else {
            context.getString(R.string.foreground_status_unconfigured)
        }

        val heartbeatStatus = buildHeartbeatStatus()

        return "$listenerStatus · $configStatus · $heartbeatStatus"
    }

    private fun buildHeartbeatStatus(): String {
        return when {
            statusStore.lastHeartbeatSuccessAt != null -> {
                val timeStr = formatTime(statusStore.lastHeartbeatSuccessAt!!)
                context.getString(R.string.foreground_status_heartbeat_success, timeStr)
            }
            statusStore.lastHeartbeatErrorMessage != null -> {
                context.getString(R.string.foreground_status_heartbeat_failed)
            }
            else -> {
                context.getString(R.string.foreground_status_heartbeat_pending)
            }
        }
    }

    private fun formatTime(instant: Instant): String {
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return localTime.format(timeFormatter)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CHANNEL_ID = "vmq_foreground_channel"
        const val NOTIFICATION_ID = 1
    }
}
