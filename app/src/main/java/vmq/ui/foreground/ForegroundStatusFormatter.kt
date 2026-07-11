package vmq.ui.foreground

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ForegroundStatus(
    val isListenerConnected: Boolean,
    val isConfigured: Boolean,
    val lastHeartbeatSuccessAt: Instant?,
    val lastHeartbeatErrorMessage: String?,
)

object ForegroundStatusFormatter {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun formatStatusText(status: ForegroundStatus): String {
        val listenerStatus = if (status.isListenerConnected) {
            "监听已连接"
        } else {
            "监听未连接"
        }

        if (!status.isConfigured) {
            return if (status.isListenerConnected) {
                "$listenerStatus · 未配置 · 等待配置"
            } else {
                "$listenerStatus · 未配置"
            }
        }

        val heartbeatStatus = formatHeartbeatStatus(status)
        return "$listenerStatus · 已配置 · $heartbeatStatus"
    }

    private fun formatHeartbeatStatus(status: ForegroundStatus): String {
        return when {
            status.lastHeartbeatErrorMessage != null -> {
                "心跳失败"
            }
            status.lastHeartbeatSuccessAt != null -> {
                val timeStr = formatTime(status.lastHeartbeatSuccessAt)
                "心跳 $timeStr"
            }
            status.isListenerConnected -> {
                "等待首次心跳"
            }
            else -> {
                "等待恢复"
            }
        }
    }

    private fun formatTime(instant: Instant): String {
        val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return localTime.format(timeFormatter)
    }
}
