package vmq.usecase

import vmq.data.AppConfig
import vmq.data.ConfigRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.test.runTest

class StartHeartbeatUseCaseTest {
    @Test
    fun `invoke returns success message when repository heartbeat succeeds`() = runTest {
        val repository = FakeConfigRepository(
            heartbeatResult = Result.success("OK"),
        )

        val result = StartHeartbeatUseCase(repository)(
            config = AppConfig(host = "https://vmq.example.com", key = "secret"),
        )

        assertEquals(StartHeartbeatUseCase.Result.Success("OK"), result)
    }

    @Test
    fun `invoke returns failure message when repository heartbeat fails`() = runTest {
        val repository = FakeConfigRepository(
            heartbeatResult = Result.failure(IllegalStateException("boom")),
        )

        val result = StartHeartbeatUseCase(repository)(
            config = AppConfig(host = "https://vmq.example.com", key = "secret"),
        )

        assertEquals(StartHeartbeatUseCase.Result.Failure, result)
    }

    private class FakeConfigRepository(
        private val heartbeatResult: Result<String>,
    ) : ConfigRepository {
        override fun loadConfig(): AppConfig = AppConfig(host = "", key = "")

        override fun saveConfig(config: AppConfig) {
        }

        override suspend fun sendHeartbeat(config: AppConfig): Result<String> {
            return heartbeatResult
        }
    }
}
