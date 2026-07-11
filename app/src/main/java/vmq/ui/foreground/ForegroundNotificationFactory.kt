package vmq.ui.foreground

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.vone.qrcode.R
import vmq.data.ConfigStore
import vmq.data.ListenerStatusStore
import vmq.ui.main.MainActivity

class ForegroundNotificationFactory(
    private val context: Context,
    private val notificationManager: NotificationManager,
    private val statusStore: ListenerStatusStore,
    private val configStore: ConfigStore,
) {

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
        val status = ForegroundStatus(
            isListenerConnected = statusStore.isListenerConnected,
            isConfigured = configStore.load().isConfigured,
            lastHeartbeatSuccessAt = statusStore.lastHeartbeatSuccessAt,
            lastHeartbeatErrorMessage = statusStore.lastHeartbeatErrorMessage,
        )
        return ForegroundStatusFormatter.formatStatusText(status)
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
