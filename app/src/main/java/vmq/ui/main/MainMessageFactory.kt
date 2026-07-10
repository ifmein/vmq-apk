package vmq.ui.main

import com.vone.qrcode.R
import vmq.ui.common.UiText
import vmq.usecase.StartHeartbeatUseCase
import vmq.usecase.SubmitConfigurationPayloadUseCase

object MainMessageFactory {
    fun configurationRejected(
        reason: SubmitConfigurationPayloadUseCase.RejectionReason,
        invalidMessage: UiText,
    ): UiText {
        return when (reason) {
            SubmitConfigurationPayloadUseCase.RejectionReason.INVALID_PAYLOAD -> invalidMessage
            SubmitConfigurationPayloadUseCase.RejectionReason.LOCALHOST_NOT_ALLOWED -> {
                UiText.StringResource(R.string.config_localhost_not_allowed)
            }
        }
    }

    fun configurationHeartbeatResult(result: StartHeartbeatUseCase.Result): UiText {
        return when (result) {
            is StartHeartbeatUseCase.Result.Success -> {
                UiText.StringResource(R.string.config_validation_success, result.message)
            }
            StartHeartbeatUseCase.Result.Failure -> {
                UiText.StringResource(R.string.config_saved_but_heartbeat_failed)
            }
        }
    }

    fun missingConfiguration(): UiText {
        return UiText.StringResource(R.string.config_missing)
    }

    fun heartbeatResult(result: StartHeartbeatUseCase.Result): UiText {
        return when (result) {
            is StartHeartbeatUseCase.Result.Success -> {
                UiText.StringResource(R.string.heartbeat_success, result.message)
            }
            StartHeartbeatUseCase.Result.Failure -> {
                UiText.StringResource(R.string.heartbeat_failed)
            }
        }
    }
}
