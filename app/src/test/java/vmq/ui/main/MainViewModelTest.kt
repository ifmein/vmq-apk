package vmq.ui.main

import app.cash.turbine.test
import com.vone.qrcode.R
import vmq.data.AppConfig
import vmq.data.ConfigRepository
import vmq.ui.common.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadSavedConfiguration updates state when config exists`() = runTest {
        val repository = FakeConfigRepository(
            loadedConfig = AppConfig(host = "https://vmq.example.com", key = "secret"),
        )
        val viewModel = MainViewModel(repository)

        viewModel.loadSavedConfiguration()

        assertEquals(repository.loadedConfig, viewModel.uiState.value.config)
        assertTrue(viewModel.uiState.value.isConfigured)
    }

    @Test
    fun `handleConfigurationPayload emits invalid message when payload is malformed`() = runTest {
        val repository = FakeConfigRepository()
        val viewModel = MainViewModel(repository)

        viewModel.handleConfigurationPayload(
            "invalid",
            UiText.StringResource(R.string.invalid_qr_configuration),
        )
        advanceUntilIdle()

        viewModel.effects.test {
            assertEquals(
                MainViewEffect.ShowToast(UiText.StringResource(R.string.invalid_qr_configuration)),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.uiState.value.isConfigured)
        assertEquals(null, repository.savedConfig)
    }

    @Test
    fun `handleConfigurationPayload saves config and verifies server when payload is valid`() = runTest {
        val repository = FakeConfigRepository(
            heartbeatResult = Result.success("OK"),
        )
        val viewModel = MainViewModel(repository)

        viewModel.handleConfigurationPayload(
            "https://vmq.example.com/secret",
            UiText.StringResource(R.string.invalid_qr_configuration),
        )
        advanceUntilIdle()

        val expectedConfig = AppConfig(host = "https://vmq.example.com", key = "secret")

        viewModel.effects.test {
            assertEquals(
                MainViewEffect.ShowToast(
                    UiText.StringResource(R.string.config_validation_success, "OK"),
                ),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(expectedConfig, repository.savedConfig)
        assertEquals(expectedConfig, repository.heartbeatConfig)
        assertEquals(expectedConfig, viewModel.uiState.value.config)
        assertTrue(viewModel.uiState.value.isConfigured)
    }

    @Test
    fun `startHeartbeat emits warning when config is missing`() = runTest {
        val repository = FakeConfigRepository()
        val viewModel = MainViewModel(repository)

        viewModel.startHeartbeat()
        advanceUntilIdle()

        viewModel.effects.test {
            assertEquals(
                MainViewEffect.ShowToast(UiText.StringResource(R.string.config_missing)),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(null, repository.heartbeatConfig)
    }

    private class FakeConfigRepository(
        val loadedConfig: AppConfig = AppConfig(host = "", key = ""),
        var heartbeatResult: Result<String> = Result.success("OK"),
    ) : ConfigRepository {
        var savedConfig: AppConfig? = null
        var heartbeatConfig: AppConfig? = null

        override fun loadConfig(): AppConfig = loadedConfig

        override fun saveConfig(config: AppConfig) {
            savedConfig = config
        }

        override suspend fun sendHeartbeat(config: AppConfig): Result<String> {
            heartbeatConfig = config
            return heartbeatResult
        }
    }
}
