package vmq.ui.notification

import android.app.Notification
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import vmq.data.ListenerStatusStore
import vmq.di.AppContainer
import vmq.notification.NotificationAction
import vmq.ui.common.UiText
import vmq.ui.foreground.ForegroundServiceController

class NeNotificationService : NotificationListenerService() {
    private val configStore by lazy { AppContainer.configStore(this) }
    private val paymentPushService by lazy { AppContainer.paymentPushService() }
    private val notificationEventHandler by lazy { AppContainer.notificationEventHandler() }
    private val listenerStatusStore by lazy { AppContainer.listenerStatusStore(this) }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        listenerStatusStore.recordNotificationEvent()

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
        Log.d(TAG, "Listener connected")
        listenerStatusStore.recordListenerConnected()
        ForegroundServiceController.refresh(this)
        showToast(NotificationMessageFactory.listenerStarted())
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "Listener disconnected")
        listenerStatusStore.recordListenerDisconnected()
        ForegroundServiceController.refresh(this)
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
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
