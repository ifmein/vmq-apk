package vmq.ui.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import vmq.di.AppContainer
import vmq.notification.NotificationAction
import vmq.ui.common.UiText

class NeNotificationService : NotificationListenerService() {
    private val configStore by lazy { AppContainer.configStore(this) }
    private val heartbeatService by lazy { AppContainer.heartbeatService() }
    private val paymentPushService by lazy { AppContainer.paymentPushService() }
    private val notificationEventHandler by lazy { AppContainer.notificationEventHandler() }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var heartbeatJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @SuppressLint("InvalidWakeLockTag")
    private fun acquireWakeLock(context: Context) {
        if (wakeLock == null) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "WakeLock",
            )
            wakeLock?.acquire()
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
            wakeLock = null
        }
    }

    private fun startHeartbeatLoop() {
        Log.d(TAG, "Starting heartbeat loop")
        if (heartbeatJob?.isActive == true) {
            return
        }

        acquireWakeLock(this)
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                val config = configStore.load()
                if (config.isConfigured) {
                    heartbeatService.sendHeartbeat(config)
                        .onFailure { error ->
                            Log.e(TAG, "Heartbeat request failed", error)
                            showToast(NotificationMessageFactory.heartbeatFailure(error.message.orEmpty()))
                        }
                } else {
                    Log.d(TAG, "Skipping heartbeat because configuration is incomplete")
                }

                delay(30 * 1000L)
            }
        }
    }

    private fun stopHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        releaseWakeLock()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG, "Received notification")

        val notification: Notification = sbn.notification ?: return
        val extras: Bundle = notification.extras ?: return
        val packageName = sbn.packageName
        val title = extras.getString(NotificationCompat.EXTRA_TITLE, "")
        val content = extras.getString(NotificationCompat.EXTRA_TEXT, "")

        Log.d(TAG, "**********************")
        Log.d(TAG, "Package:$packageName")
        Log.d(TAG, "Title:$title")
        Log.d(TAG, "Content:$content")
        Log.d(TAG, "**********************")

        when (val action = notificationEventHandler.handle(packageName, title, content)) {
            NotificationAction.Ignore -> Unit
            NotificationAction.SelfTestSucceeded -> showToast(NotificationMessageFactory.selfTestSucceeded())
            is NotificationAction.PaymentDetected -> sendPaymentCallback(action)
            is NotificationAction.AmountParseFailed -> {
                if (configStore.load().isConfigured) {
                    showToast(NotificationMessageFactory.amountParseFailed(action.platform))
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
    }

    override fun onListenerConnected() {
        startHeartbeatLoop()
        showToast(NotificationMessageFactory.listenerStarted())
    }

    override fun onListenerDisconnected() {
        stopHeartbeatLoop()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        stopHeartbeatLoop()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun sendPaymentCallback(action: NotificationAction.PaymentDetected) {
        val config = configStore.load()
        if (!config.isConfigured) {
            Log.w(TAG, "Skipping payment callback because configuration is incomplete")
            return
        }

        Log.d(TAG, "Payment detected: ${action.event.type} amount=${action.event.amount}")
        serviceScope.launch {
            paymentPushService.sendPayment(config, action.event)
                .onSuccess { response ->
                    Log.d(TAG, "Payment callback response: $response")
                }
                .onFailure { error ->
                    Log.e(TAG, "Payment callback request failed", error)
                }
        }
    }

    private fun showToast(message: UiText) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message.resolve(applicationContext), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "NeNotificationService"
    }
}
