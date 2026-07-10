package vmq.parser

import vmq.model.PaymentEvent
import vmq.model.PaymentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationParserTest {
    @Test
    fun `parse returns alipay event for matching content`() {
        val result = PaymentNotificationParser.parse(
            packageName = "com.eg.android.AlipayGphone",
            title = "支付宝",
            content = "通过扫码向你付款12.34元",
        )

        assertEquals(PaymentEvent(PaymentType.ALIPAY, 12.34), result)
    }

    @Test
    fun `parse returns wechat event for supported title`() {
        val result = PaymentNotificationParser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            content = "微信支付收款8.88元",
        )

        assertEquals(PaymentEvent(PaymentType.WECHAT, 8.88), result)
    }

    @Test
    fun `parse returns null for blank content`() {
        val result = PaymentNotificationParser.parse(
            packageName = "com.tencent.mm",
            title = "微信支付",
            content = "",
        )

        assertNull(result)
    }

    @Test
    fun `parse returns null for unsupported package`() {
        val result = PaymentNotificationParser.parse(
            packageName = "com.example.other",
            title = "微信支付",
            content = "微信支付收款8.88元",
        )

        assertNull(result)
    }

    @Test
    fun `parse returns wechat event for alternate supported title`() {
        val result = PaymentNotificationParser.parse(
            packageName = "com.tencent.mm",
            title = "微信收款助手",
            content = "微信支付收款8.88元",
        )

        assertEquals(PaymentEvent(PaymentType.WECHAT, 8.88), result)
    }

    @Test
    fun `parse returns null when wechat title is unsupported`() {
        val result = PaymentNotificationParser.parse(
            packageName = "com.tencent.mm",
            title = "服务通知",
            content = "微信支付收款8.88元",
        )

        assertNull(result)
    }

    @Test
    fun `parse returns null when content does not include amount`() {
        val result = PaymentNotificationParser.parse(
            packageName = "com.eg.android.AlipayGphone",
            title = "支付宝",
            content = "成功收款",
        )

        assertNull(result)
    }

    @Test
    fun `isSelfTestNotification returns true only for app package and expected content`() {
        assertTrue(
            PaymentNotificationParser.isSelfTestNotification(
                packageName = "com.vone.qrcode",
                content = PaymentNotificationParser.SELF_TEST_MESSAGE,
            ),
        )
        assertFalse(
            PaymentNotificationParser.isSelfTestNotification(
                packageName = "com.tencent.mm",
                content = PaymentNotificationParser.SELF_TEST_MESSAGE,
            ),
        )
    }
}
