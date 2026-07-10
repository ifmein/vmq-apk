package vmq.usecase

import vmq.data.AppConfig
import vmq.data.ConfigRepository

class StartHeartbeatUseCase(
    private val repository: ConfigRepository,
) {
    suspend operator fun invoke(config: AppConfig): Result {
        val result = repository.sendHeartbeat(config)
        return result.fold(
            onSuccess = { message -> Result.Success(message) },
            onFailure = { Result.Failure },
        )
    }

    sealed interface Result {
        data class Success(val message: String) : Result

        data object Failure : Result
    }
}
