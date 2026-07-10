package vmq.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import vmq.data.ConfigRepository
import vmq.usecase.LoadSavedConfigurationUseCase
import vmq.usecase.StartHeartbeatUseCase
import vmq.usecase.SubmitConfigurationPayloadUseCase

class MainViewModelFactory(
    private val repository: ConfigRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                loadSavedConfigurationUseCase = LoadSavedConfigurationUseCase(repository),
                submitConfigurationPayloadUseCase = SubmitConfigurationPayloadUseCase(repository),
                startHeartbeatUseCase = StartHeartbeatUseCase(repository),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
