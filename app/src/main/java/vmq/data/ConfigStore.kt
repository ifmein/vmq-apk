package vmq.data

import android.content.Context

class ConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppConfig {
        return AppConfig(
            host = preferences.getString(PREF_HOST, "").orEmpty(),
            key = preferences.getString(PREF_KEY, "").orEmpty(),
        )
    }

    fun save(config: AppConfig) {
        preferences.edit()
            .putString(PREF_HOST, config.host)
            .putString(PREF_KEY, config.key)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "vone"
        private const val PREF_HOST = "host"
        private const val PREF_KEY = "key"
    }
}
