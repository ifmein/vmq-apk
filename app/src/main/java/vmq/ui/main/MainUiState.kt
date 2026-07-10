package vmq.ui.main

import vmq.data.AppConfig

data class MainUiState(
    val config: AppConfig = AppConfig(host = "", key = ""),
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
)
