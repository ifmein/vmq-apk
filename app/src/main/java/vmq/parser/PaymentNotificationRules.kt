package vmq.parser

object PaymentNotificationRules {
    const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
    const val WECHAT_PACKAGE = "com.tencent.mm"
    const val APP_PACKAGE = "com.vone.qrcode"

    const val ALIPAY_PLATFORM = "支付宝"
    const val WECHAT_PLATFORM = "微信"

    private val weChatPaymentTitles = setOf(
        "微信支付",
        "微信收款助手",
        "微信收款商业版",
    )

    private val alipayPaymentKeywords = setOf(
        "通过扫码向你付款",
        "成功收款",
    )

    fun isWeChatPaymentTitle(title: String?): Boolean {
        return title != null && title in weChatPaymentTitles
    }

    fun containsAlipayPaymentKeyword(content: String): Boolean {
        return alipayPaymentKeywords.any(content::contains)
    }
}
