package vmq.data

data class AppConfig(
    val host: String,
    val key: String,
) {
    val isConfigured: Boolean
        get() = host.isNotBlank() && key.isNotBlank()
}
