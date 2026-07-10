package vmq.usecase

import vmq.data.AppConfig
import vmq.data.ConfigRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadSavedConfigurationUseCaseTest {
    @Test
    fun `invoke returns config loaded from repository`() {
        val repository = FakeConfigRepository(
            loadedConfig = AppConfig(host = "https://vmq.example.com", key = "secret"),
        )

        val result = LoadSavedConfigurationUseCase(repository)()

        assertEquals(repository.loadedConfig, result)
    }

    private class FakeConfigRepository(
        val loadedConfig: AppConfig,
    ) : ConfigRepository {
        override fun loadConfig(): AppConfig = loadedConfig

        override fun saveConfig(config: AppConfig) {
        }

        override suspend fun sendHeartbeat(config: AppConfig): Result<String> {
            return Result.success("OK")
        }
    }
}
