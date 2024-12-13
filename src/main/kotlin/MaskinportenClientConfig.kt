package no.nav.helsearbeidsgiver.maskinporten

import java.time.Instant
import java.util.Date

interface MaskinportenClientConfig {
    val scope: String
    val clientId: String
    val issuer: String
    val endpoint: String
    val additionalClaims: Map<String, Any>?
    fun getJwtAssertion(): String
    fun currentTime(): Date = Date.from(Instant.now())
}

fun getConsumerOrgClaim(orgnr: String) = mapOf("consumer_orgno" to orgnr)

fun getSystembrukerClaim(orgNr: String) = mapOf(
    "authorization_details" to listOf(
        mapOf(
            "type" to "urn:altinn:systemuser",
            "systemuser_org" to mapOf(
                "authority" to "iso6523-actorid-upis",
                "ID" to "0192:$orgNr"
            )
        )
    )
)
