package vmq.notification

import vmq.parser.PaymentNotificationParser
import vmq.parser.PaymentNotificationRules

class NotificationEventHandler {
    fun handle(packageName: String, title: String?, content: String?): NotificationAction {
        if (PaymentNotificationParser.isSelfTestNotification(packageName, content)) {
            return NotificationAction.SelfTestSucceeded
        }

        val paymentEvent = PaymentNotificationParser.parse(packageName, title, content)
        if (paymentEvent != null) {
            return NotificationAction.PaymentDetected(paymentEvent)
        }

        if (content.isNullOrEmpty()) {
            return NotificationAction.Ignore
        }

        return when {
            packageName == PaymentNotificationRules.ALIPAY_PACKAGE -> {
                NotificationAction.AmountParseFailed(PaymentNotificationRules.ALIPAY_PLATFORM)
            }
            packageName == PaymentNotificationRules.WECHAT_PACKAGE &&
                PaymentNotificationRules.isWeChatPaymentTitle(title) -> {
                NotificationAction.AmountParseFailed(PaymentNotificationRules.WECHAT_PLATFORM)
            }
            else -> NotificationAction.Ignore
        }
    }
}
