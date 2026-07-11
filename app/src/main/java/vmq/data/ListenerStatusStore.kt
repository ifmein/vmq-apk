package vmq.data

import android.content.Context
import android.content.SharedPreferences
import java.time.Instant

class ListenerStatusStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isListenerConnected: Boolean
        get() = prefs.getBoolean(KEY_LISTENER_CONNECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_LISTENER_CONNECTED, value).apply()

    var lastListenerConnectedAt: Instant?
        get() = prefs.getLong(KEY_LISTENER_CONNECTED_AT, 0).takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }
        set(value) = prefs.edit().putLong(KEY_LISTENER_CONNECTED_AT, value?.toEpochMilli() ?: 0).apply()

    var lastNotificationEventAt: Instant?
        get() = prefs.getLong(KEY_NOTIFICATION_EVENT_AT, 0).takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }
        set(value) = prefs.edit().putLong(KEY_NOTIFICATION_EVENT_AT, value?.toEpochMilli() ?: 0).apply()

    var lastHeartbeatSuccessAt: Instant?
        get() = prefs.getLong(KEY_HEARTBEAT_SUCCESS_AT, 0).takeIf { it > 0 }?.let { Instant.ofEpochMilli(it) }
        set(value) = prefs.edit().putLong(KEY_HEARTBEAT_SUCCESS_AT, value?.toEpochMilli() ?: 0).apply()

    var lastHeartbeatErrorMessage: String?
        get() = prefs.getString(KEY_HEARTBEAT_ERROR_MESSAGE, null)
        set(value) = prefs.edit().putString(KEY_HEARTBEAT_ERROR_MESSAGE, value).apply()

    fun recordListenerConnected() {
        isListenerConnected = true
        lastListenerConnectedAt = Instant.now()
    }

    fun recordListenerDisconnected() {
        isListenerConnected = false
    }

    fun recordNotificationEvent() {
        lastNotificationEventAt = Instant.now()
    }

    fun recordHeartbeatSuccess() {
        lastHeartbeatSuccessAt = Instant.now()
        lastHeartbeatErrorMessage = null
    }

    fun recordHeartbeatFailure(message: String) {
        lastHeartbeatSuccessAt = null
        lastHeartbeatErrorMessage = message
    }

    companion object {
        private const val PREFS_NAME = "listener_status"
        private const val KEY_LISTENER_CONNECTED = "listener_connected"
        private const val KEY_LISTENER_CONNECTED_AT = "listener_connected_at"
        private const val KEY_NOTIFICATION_EVENT_AT = "notification_event_at"
        private const val KEY_HEARTBEAT_SUCCESS_AT = "heartbeat_success_at"
        private const val KEY_HEARTBEAT_ERROR_MESSAGE = "heartbeat_error_message"
    }
}
