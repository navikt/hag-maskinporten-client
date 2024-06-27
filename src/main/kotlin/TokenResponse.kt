package no.nav.helsearbeidsgiver.maskinporten

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * {
 *   "access_token" : "IxC0B76vlWl3fiQhAwZUmD0hr_PPwC9hSIXRdoUslPU=",
 *   "token_type" : "Bearer",
 *   "expires_in" : 599,
 *   "scope" : "difitest:test1"
 * }
 */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresInSeconds: Long,
    val scope: String
)
class TokenResponseWrapper(val tokenResponse: TokenResponse) {

    private val issueTime = System.currentTimeMillis() / 1000

    val remainingTimeInSeconds: Long
        get() = tokenResponse.expiresInSeconds - (System.currentTimeMillis() / 1000 - issueTime)

    val remainingTimePercentage: Double
        get() = (remainingTimeInSeconds.toDouble() / tokenResponse.expiresInSeconds) * 100
}
