package vmq.parser

import vmq.data.AppConfig

object ConfigurationPayloadParser {
    fun parse(payload: String): AppConfig? {
        if (payload.isBlank()) {
            return null
        }

        val trimmedPayload = payload.trim()
        val schemeSeparatorIndex = trimmedPayload.indexOf("://")
        val minimumSeparatorIndex = if (schemeSeparatorIndex >= 0) {
            schemeSeparatorIndex + 3
        } else {
            1
        }

        val separatorIndex = trimmedPayload.lastIndexOf('/')
        if (separatorIndex < minimumSeparatorIndex || separatorIndex >= trimmedPayload.length - 1) {
            return null
        }

        val host = trimTrailingSlash(trimmedPayload.substring(0, separatorIndex).trim())
        val key = trimmedPayload.substring(separatorIndex + 1).trim()
        if (host.isEmpty() || key.isEmpty()) {
            return null
        }

        return AppConfig(host = host, key = key)
    }

    private fun trimTrailingSlash(value: String): String {
        var trimmedValue = value.trim()
        while (trimmedValue.endsWith('/')) {
            trimmedValue = trimmedValue.dropLast(1)
        }
        return trimmedValue
    }
}
