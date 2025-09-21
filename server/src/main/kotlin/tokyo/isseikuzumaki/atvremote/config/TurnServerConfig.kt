package tokyo.isseikuzumaki.atvremote.config

import tokyo.isseikuzumaki.atvremote.DotEnv


/**
 * Configuration for Cloudflare TURN server access
 */
data class TurnServerConfig(
    val appId: String,
    val apiToken: String,
    val baseUrl: String = "https://rtc.live.cloudflare.com"
) {
    companion object {
        /**
         * Creates TurnServerConfig from build configuration
         */
        fun fromSystemProperties(): TurnServerConfig {
            System.getProperties().keys.forEach {
                println("System Property: $it = ${System.getProperty(it.toString())}")
            }
            return TurnServerConfig(
                appId = DotEnv.CLOUDFLARE_REALTIME_TURN_APP_ID,
                apiToken = DotEnv.CLOUDFLARE_REALTIME_TURN_API_TOKEN
            )
        }
    }
    
    /**
     * Validates that required configuration is present
     */
    fun isValid(): Boolean {
        return appId.isNotBlank() && apiToken.isNotBlank()
    }
}