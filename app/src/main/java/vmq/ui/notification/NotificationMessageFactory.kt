package vmq.ui.notification

import com.vone.qrcode.R
import vmq.ui.common.UiText

object NotificationMessageFactory {
    fun heartbeatFailure(errorMessage: String): UiText {
        return UiText.StringResource(R.string.notification_heartbeat_failed, errorMessage)
    }

    fun selfTestSucceeded(): UiText {
        return UiText.StringResource(R.string.notification_listener_ok)
    }

    fun amountParseFailed(platform: String): UiText {
        return UiText.StringResource(R.string.notification_amount_parse_failed, platform)
    }

    fun listenerStarted(): UiText {
        return UiText.StringResource(R.string.notification_listener_started)
    }
}
