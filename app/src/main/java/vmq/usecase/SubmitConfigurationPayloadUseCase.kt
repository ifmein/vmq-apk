package vmq.usecase

import vmq.data.AppConfig
import vmq.data.ConfigRepository
import vmq.parser.ConfigurationPayloadParser

class SubmitConfigurationPayloadUseCase(
    private val repository: ConfigRepository,
) {
    operator fun invoke(payload: String): Result {
        val config = ConfigurationPayloadParser.parse(payload)
        if (config == null) {
            return Result.Rejected(RejectionReason.INVALID_PAYLOAD)
        }

        if (config.host.contains("localhost", ignoreCase = true)) {
            return Result.Rejected(RejectionReason.LOCALHOST_NOT_ALLOWED)
        }

        repository.saveConfig(config)
        return Result.Accepted(config)
    }

    enum class RejectionReason {
        INVALID_PAYLOAD,
        LOCALHOST_NOT_ALLOWED,
    }

    sealed interface Result {
        data class Accepted(val config: AppConfig) : Result

        data class Rejected(val reason: RejectionReason) : Result
    }
}
