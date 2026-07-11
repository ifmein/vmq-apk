package vmq.ui.foreground

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import vmq.data.ConfigStore
import vmq.data.ListenerStatusStore
import vmq.di.AppContainer
import vmq.network.HeartbeatService

class PersistentForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var heartbeatJob: Job? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private lateinit var notificationFactory: ForegroundNotificationFactory
    private lateinit var statusStore: ListenerStatusStore
    private lateinit var configStore: ConfigStore
    private lateinit var heartbeatService: HeartbeatService

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PersistentForegroundService created")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        statusStore = AppContainer.listenerStatusStore(this)
        configStore = AppContainer.configStore(this)
        heartbeatService = AppContainer.heartbeatService()

        notificationFactory = ForegroundNotificationFactory(
            context = this,
            notificationManager = notificationManager,
            statusStore = statusStore,
        )
        notificationFactory.createOrUpdateChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "PersistentForegroundService started")

        acquireWakeLock()
        startForegroundWithNotification()

        if (heartbeatJob?.isActive != true) {
            startHeartbeatLoop()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "PersistentForegroundService destroyed")
        stopHeartbeatLoop()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val notification = notificationFactory.buildNotification()
        ServiceCompat.startForeground(
            this,
            ForegroundNotificationFactory.NOTIFICATION_ID,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            @Suppress("BatteryLife")
            wakeLock = powerManager.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK or android.os.PowerManager.ON_AFTER_RELEASE,
                "$TAG:WakeLock",
            )
            wakeLock?.acquire(HEARTBEAT_INTERVAL_MS * HEARTBEAT_LOOP_COUNT)
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
        heartbeatJob = serviceScope.launch {
            var loopCount = 0
            while (isActive) {
                val config = configStore.load()
                if (config.isConfigured) {
                    heartbeatService.sendHeartbeat(config)
                        .onSuccess {
                            Log.d(TAG, "Heartbeat success")
                            statusStore.recordHeartbeatSuccess()
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Heartbeat failed", error)
                            statusStore.recordHeartbeatFailure(error.message.orEmpty())
                        }
                } else {
                    Log.d(TAG, "Skipping heartbeat because configuration is incomplete")
                }

                loopCount++
                if (loopCount >= HEARTBEAT_LOOP_COUNT) {
                    loopCount = 0
                    releaseWakeLock()
                    acquireWakeLock()
                    refreshNotification()
                }

                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    private fun stopHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun refreshNotification() {
        val notification = notificationFactory.buildNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(ForegroundNotificationFactory.NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "PersistentForegroundService"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private const val HEARTBEAT_LOOP_COUNT = 2
    }
}
