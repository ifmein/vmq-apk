package vmq.usecase

import vmq.data.AppConfig
import vmq.data.ConfigRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubmitConfigurationPayloadUseCaseTest {
    @Test
    fun `invoke returns rejected when payload is invalid`() {
        val repository = FakeConfigRepository()

        val result = SubmitConfigurationPayloadUseCase(repository)("invalid")

        assertEquals(
            SubmitConfigurationPayloadUseCase.Result.Rejected(
                SubmitConfigurationPayloadUseCase.RejectionReason.INVALID_PAYLOAD,
            ),
            result,
        )
        assertNull(repository.savedConfig)
    }

    @Test
    fun `invoke returns rejected when host points to localhost`() {
        val repository = FakeConfigRepository()

        val result = SubmitConfigurationPayloadUseCase(repository)("http://localhost:8080/secret")

        assertEquals(
            SubmitConfigurationPayloadUseCase.Result.Rejected(
                SubmitConfigurationPayloadUseCase.RejectionReason.LOCALHOST_NOT_ALLOWED,
            ),
            result,
        )
        assertNull(repository.savedConfig)
    }

    @Test
    fun `invoke saves config and returns accepted when payload is valid`() {
        val repository = FakeConfigRepository()

        val result = SubmitConfigurationPayloadUseCase(repository)("https://vmq.example.com/secret")

        val expectedConfig = AppConfig(host = "https://vmq.example.com", key = "secret")
        assertEquals(SubmitConfigurationPayloadUseCase.Result.Accepted(expectedConfig), result)
        assertEquals(expectedConfig, repository.savedConfig)
    }

    private class FakeConfigRepository : ConfigRepository {
        var savedConfig: AppConfig? = null

        override fun loadConfig(): AppConfig = AppConfig(host = "", key = "")

        override fun saveConfig(config: AppConfig) {
            savedConfig = config
        }

        override suspend fun sendHeartbeat(config: AppConfig): Result<String> {
            return Result.success("OK")
        }
    }
}
