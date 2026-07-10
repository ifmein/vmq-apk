package vmq.network

object ApiUrlBuilder {
    fun buildHeartBeatUrl(hostValue: String, timestamp: String, sign: String): String {
        return build(hostValue, "appHeart?t=$timestamp&sign=$sign")
    }

    fun buildAppPushUrl(hostValue: String, timestamp: String, type: Int, price: Double, sign: String): String {
        return build(hostValue, "appPush?t=$timestamp&type=$type&price=$price&sign=$sign")
    }

    fun build(hostValue: String, pathAndQuery: String): String {
        return normalizeHost(hostValue) + "/" + pathAndQuery
    }

    private fun normalizeHost(hostValue: String): String {
        val trimmedHost = trimTrailingSlash(hostValue.trim())
        return if (trimmedHost.startsWith("http://") || trimmedHost.startsWith("https://")) {
            trimmedHost
        } else {
            "http://$trimmedHost"
        }
    }

    private fun trimTrailingSlash(value: String): String {
        if (value.isEmpty()) {
            return ""
        }

        var trimmedValue = value.trim()
        while (trimmedValue.endsWith('/')) {
            trimmedValue = trimmedValue.dropLast(1)
        }
        return trimmedValue
    }
}
