package vmq.ui.main

import vmq.data.AppConfig

@Deprecated("Use vmq.parser.ConfigurationPayloadParser instead")
object ConfigurationPayloadParser {
    fun parse(payload: String): AppConfig? {
        return vmq.parser.ConfigurationPayloadParser.parse(payload)
    }
}
