package vmq.data

import vmq.network.HeartbeatService

class DefaultConfigRepository(
    private val configStore: ConfigStore,
    private val heartbeatService: HeartbeatService,
) : ConfigRepository {
    override fun loadConfig(): AppConfig = configStore.load()

    override fun saveConfig(config: AppConfig) {
        configStore.save(config)
    }

    override suspend fun sendHeartbeat(config: AppConfig): Result<String> {
        return heartbeatService.sendHeartbeat(config)
    }
}
