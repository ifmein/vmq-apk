package vmq.di

import android.content.Context
import vmq.data.ConfigRepository
import vmq.data.ConfigStore
import vmq.data.DefaultConfigRepository
import vmq.data.ListenerStatusStore
import vmq.network.HeartbeatService
import vmq.network.PaymentPushService
import vmq.notification.NotificationEventHandler
import okhttp3.OkHttpClient

object AppContainer {
    private val okHttpClient: OkHttpClient by lazy { OkHttpClient() }
    private val heartbeatService: HeartbeatService by lazy { HeartbeatService(okHttpClient = okHttpClient) }
    private val paymentPushService: PaymentPushService by lazy { PaymentPushService(okHttpClient = okHttpClient) }
    private val notificationEventHandler: NotificationEventHandler by lazy { NotificationEventHandler() }

    fun configStore(context: Context): ConfigStore {
        return ConfigStore(context.applicationContext)
    }

    fun configRepository(context: Context): ConfigRepository {
        return DefaultConfigRepository(
            configStore = configStore(context),
            heartbeatService = heartbeatService,
        )
    }

    fun heartbeatService(): HeartbeatService = heartbeatService

    fun paymentPushService(): PaymentPushService = paymentPushService

    fun notificationEventHandler(): NotificationEventHandler = notificationEventHandler

    fun listenerStatusStore(context: Context): ListenerStatusStore {
        return ListenerStatusStore(context.applicationContext)
    }
}
