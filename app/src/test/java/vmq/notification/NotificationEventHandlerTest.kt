package vmq.notification

import com.vone.qrcode.BuildConfig
import vmq.model.PaymentEvent
import vmq.model.PaymentType
import vmq.parser.PaymentNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationEventHandlerTest {
    private val handler = NotificationEventHandler()

    @Test
    fun `handle returns self test action for internal test notification`() {
        val result = handler.handle(
            packageName = BuildConfig.APPLICATION_ID,
            title = "V免签测试推送",
            content = PaymentNotificationParser.SELF_TEST_MESSAGE,
        )

        assertEquals(NotificationAction.SelfTestSucceeded, result)
    }

    @Test
    fun `handle returns payment detected action for valid payment notification`() {
        val result = handler.handle(
            packageName = "com.tencent.mm",
            title = "微信支付",
            content = "微信支付收款8.88元",
        )

        assertEquals(
            NotificationAction.PaymentDetected(PaymentEvent(PaymentType.WECHAT, 8.88)),
            result,
        )
    }

    @Test
    fun `handle returns payment detected action for personal wechat collection code notification`() {
        val result = handler.handle(
            packageName = "com.tencent.mm",
            title = "微信支付",
            content = "个人收款码到账¥0.03",
        )

        assertEquals(
            NotificationAction.PaymentDetected(PaymentEvent(PaymentType.WECHAT, 0.03)),
            result,
        )
    }

    @Test
    fun `handle returns amount parse failed for supported payment notification without amount`() {
        val result = handler.handle(
            packageName = "com.tencent.mm",
            title = "微信支付",
            content = "微信支付到账通知",
        )

        assertEquals(NotificationAction.AmountParseFailed("微信"), result)
    }

    @Test
    fun `handle returns ignore when content is blank`() {
        val result = handler.handle(
            packageName = "com.tencent.mm",
            title = "微信支付",
            content = "",
        )

        assertEquals(NotificationAction.Ignore, result)
    }

    @Test
    fun `handle returns ignore for unrelated notification`() {
        val result = handler.handle(
            packageName = "com.example.other",
            title = "普通通知",
            content = "hello",
        )

        assertEquals(NotificationAction.Ignore, result)
    }
}
