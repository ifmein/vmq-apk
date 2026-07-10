package vmq.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import vmq.data.ConfigRepository
import vmq.ui.common.UiText
import vmq.usecase.LoadSavedConfigurationUseCase
import vmq.usecase.StartHeartbeatUseCase
import vmq.usecase.SubmitConfigurationPayloadUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val loadSavedConfigurationUseCase: LoadSavedConfigurationUseCase,
    private val submitConfigurationPayloadUseCase: SubmitConfigurationPayloadUseCase,
    private val startHeartbeatUseCase: StartHeartbeatUseCase,
) : ViewModel() {
    constructor(repository: ConfigRepository) : this(
        loadSavedConfigurationUseCase = LoadSavedConfigurationUseCase(repository),
        submitConfigurationPayloadUseCase = SubmitConfigurationPayloadUseCase(repository),
        startHeartbeatUseCase = StartHeartbeatUseCase(repository),
    )
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<MainViewEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    fun loadSavedConfiguration() {
        val config = loadSavedConfigurationUseCase()
        _uiState.value = MainUiState(
            config = config,
            isConfigured = config.isConfigured,
        )
    }

    fun handleConfigurationPayload(payload: String, invalidMessage: UiText) {
        viewModelScope.launch {
            when (val result = submitConfigurationPayloadUseCase(payload)) {
                is SubmitConfigurationPayloadUseCase.Result.Rejected -> {
                    effectChannel.send(
                        MainViewEffect.ShowToast(
                            MainMessageFactory.configurationRejected(result.reason, invalidMessage),
                        ),
                    )
                }
                is SubmitConfigurationPayloadUseCase.Result.Accepted -> {
                    _uiState.value = _uiState.value.copy(
                        config = result.config,
                        isConfigured = true,
                    )
                    sendConfigurationHeartbeat(result.config)
                }
            }
        }
    }

    fun startHeartbeat() {
        viewModelScope.launch {
            val config = _uiState.value.config
            if (!config.isConfigured) {
                effectChannel.send(MainViewEffect.ShowToast(MainMessageFactory.missingConfiguration()))
                return@launch
            }

            sendHeartbeat(config)
        }
    }

    private suspend fun sendConfigurationHeartbeat(config: vmq.data.AppConfig) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val result = startHeartbeatUseCase(config)
        _uiState.value = _uiState.value.copy(isLoading = false)

        effectChannel.send(
            MainViewEffect.ShowToast(MainMessageFactory.configurationHeartbeatResult(result)),
        )
    }

    private suspend fun sendHeartbeat(config: vmq.data.AppConfig) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val result = startHeartbeatUseCase(config)
        _uiState.value = _uiState.value.copy(isLoading = false)

        effectChannel.send(
            MainViewEffect.ShowToast(MainMessageFactory.heartbeatResult(result)),
        )
    }
}
