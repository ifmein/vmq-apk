package vmq.usecase

import vmq.data.AppConfig
import vmq.data.ConfigRepository

class LoadSavedConfigurationUseCase(
    private val repository: ConfigRepository,
) {
    operator fun invoke(): AppConfig {
        return repository.loadConfig()
    }
}
