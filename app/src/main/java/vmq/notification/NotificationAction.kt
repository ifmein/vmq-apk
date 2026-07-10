package vmq.notification

import vmq.model.PaymentEvent

sealed interface NotificationAction {
    data object Ignore : NotificationAction

    data object SelfTestSucceeded : NotificationAction

    data class PaymentDetected(val event: PaymentEvent) : NotificationAction

    data class AmountParseFailed(val platform: String) : NotificationAction
}
