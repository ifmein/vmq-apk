package vmq.ui.foreground

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ForegroundStatusFormatterTest {

    @Test
    fun `connected configured with heartbeat shows success time`() {
        val instant = Instant.parse("2024-01-15T12:30:45Z")
        val status = ForegroundStatus(
            isListenerConnected = true,
            lastHeartbeatSuccessAt = instant,
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听已连接 · 已配置 · 心跳 20:30:45", result)
    }

    @Test
    fun `disconnected configured shows waiting for recovery`() {
        val status = ForegroundStatus(
            isListenerConnected = false,
            lastHeartbeatSuccessAt = Instant.parse("2024-01-15T12:00:00Z"),
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听未连接 · 已配置 · 心跳 20:00:00", result)
    }

    @Test
    fun `connected unconfigured shows waiting for config`() {
        val status = ForegroundStatus(
            isListenerConnected = true,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听已连接 · 未配置 · 等待首次心跳", result)
    }

    @Test
    fun `disconnected unconfigured shows both missing`() {
        val status = ForegroundStatus(
            isListenerConnected = false,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听未连接 · 未配置 · 等待首次心跳", result)
    }

    @Test
    fun `heartbeat failure shows failed status`() {
        val status = ForegroundStatus(
            isListenerConnected = true,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = "Connection timeout",
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听已连接 · 已配置 · 心跳失败", result)
    }

    @Test
    fun `configured via error message still shows configured`() {
        val status = ForegroundStatus(
            isListenerConnected = false,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = "Some error",
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听未连接 · 已配置 · 心跳失败", result)
    }
}
