package vmq.parser

import vmq.model.PaymentEvent
import vmq.model.PaymentType

object PaymentNotificationParser {
    const val SELF_TEST_MESSAGE = "这是一条测试推送信息，如果程序正常，则会提示监听权限正常"

    fun parse(packageName: String, title: String?, content: String?): PaymentEvent? {
        return when (packageName) {
            PaymentNotificationRules.ALIPAY_PACKAGE -> parseAlipay(title, content)
            PaymentNotificationRules.WECHAT_PACKAGE -> content?.let { parseWeChat(title, it) }
            else -> null
        }
    }

    fun isSelfTestNotification(packageName: String, content: String?): Boolean {
        return packageName == PaymentNotificationRules.APP_PACKAGE && content == SELF_TEST_MESSAGE
    }

    private fun parseAlipay(title: String?, content: String?): PaymentEvent? {
        val notificationText = listOfNotNull(title, content)
            .filter(String::isNotBlank)
            .joinToString(" ")
        if (!PaymentNotificationRules.containsAlipayPaymentKeyword(notificationText)) {
            return null
        }

        val amount = extractAmount(notificationText) ?: return null
        return PaymentEvent(PaymentType.ALIPAY, amount)
    }

    private fun parseWeChat(title: String?, content: String): PaymentEvent? {
        if (!PaymentNotificationRules.isWeChatPaymentTitle(title)) {
            return null
        }

        val amount = extractAmount(content) ?: return null
        return PaymentEvent(PaymentType.WECHAT, amount)
    }

    private fun extractAmount(content: String): Double? {
        val lastNumber = content
            .replace(Regex("[^0-9.]"), ",")
            .split(",")
            .filter { it.isNotEmpty() }
            .lastOrNull()

        return lastNumber?.toDoubleOrNull()
    }
}
