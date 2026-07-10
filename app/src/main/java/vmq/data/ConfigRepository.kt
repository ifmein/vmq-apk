package vmq.data

interface ConfigRepository {
    fun loadConfig(): AppConfig

    fun saveConfig(config: AppConfig)

    suspend fun sendHeartbeat(config: AppConfig): Result<String>
}
