package vmq.ui.foreground

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class ForegroundStatusFormatterTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `connected configured with heartbeat shows success time`() {
        val instant = Instant.parse("2024-01-15T12:30:45Z")
        val status = ForegroundStatus(
            isListenerConnected = true,
            isConfigured = true,
            lastHeartbeatSuccessAt = instant,
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听已连接 · 已配置 · 心跳 20:30:45", result)
    }

    @Test
    fun `disconnected configured shows latest heartbeat time when success exists`() {
        val status = ForegroundStatus(
            isListenerConnected = false,
            isConfigured = true,
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
            isConfigured = false,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听已连接 · 未配置 · 等待配置", result)
    }

    @Test
    fun `disconnected unconfigured shows both missing`() {
        val status = ForegroundStatus(
            isListenerConnected = false,
            isConfigured = false,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听未连接 · 未配置", result)
    }

    @Test
    fun `heartbeat failure shows failed status`() {
        val status = ForegroundStatus(
            isListenerConnected = true,
            isConfigured = true,
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
            isConfigured = true,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = "Some error",
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听未连接 · 已配置 · 心跳失败", result)
    }

    @Test
    fun `connected configured without heartbeat shows pending`() {
        val status = ForegroundStatus(
            isListenerConnected = true,
            isConfigured = true,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听已连接 · 已配置 · 等待首次心跳", result)
    }

    @Test
    fun `disconnected configured without heartbeat shows waiting recovery`() {
        val status = ForegroundStatus(
            isListenerConnected = false,
            isConfigured = true,
            lastHeartbeatSuccessAt = null,
            lastHeartbeatErrorMessage = null,
        )

        val result = ForegroundStatusFormatter.formatStatusText(status)

        assertEquals("监听未连接 · 已配置 · 等待恢复", result)
    }
}
