package tokyo.isseikuzumaki.atvremote.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tokyo.isseikuzumaki.atvremote.config.TurnServerConfig

/**
 * Data classes for TURN server API responses
 */
@Serializable
data class IceServer(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

@Serializable
data class CloudflareTurnResponse(
    val iceServers: List<IceServer>
)

@Serializable
data class TurnCredentialsRequest(
    val ttl: Int = 86400
)

/**
 * Service for accessing Cloudflare TURN servers
 */
class TurnServerService(private val config: TurnServerConfig) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Retrieves TURN server credentials from Cloudflare
     * @param ttl Time to live for the credentials in seconds (default: 86400 = 24 hours)
     * @return Result containing CloudflareTurnResponse with ICE servers information
     */
    suspend fun getTurnCredentials(ttl: Int = 86400): Result<CloudflareTurnResponse> {
        return try {
            if (!config.isValid()) {
                return Result.failure(IllegalStateException("TURN server configuration is invalid"))
            }

            val response = httpClient.post("${config.baseUrl}/v1/turn/keys/${config.appId}/credentials/generate-ice-servers") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${config.apiToken}")
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
                setBody(TurnCredentialsRequest(ttl))
            }

            if (response.status.isSuccess()) {
                val turnResponse: CloudflareTurnResponse = response.body()
                Result.success(turnResponse)
            } else {
                Result.failure(Exception("HTTP ${response.status.value}: ${response.status.description}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Closes the HTTP client and releases resources
     */
    fun close() {
        httpClient.close()
    }
}