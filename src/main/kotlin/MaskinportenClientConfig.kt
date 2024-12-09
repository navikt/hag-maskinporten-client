package no.nav.helsearbeidsgiver.maskinporten

interface MaskinportenClientConfig {
    val scope: String
    val clientId: String
    val issuer: String
    val endpoint: String
    val additionalClaims: Map<String, Any>?
    fun getJwtAssertion(): String
}

fun getConsumerOrgClaim(orgNr: String) = mapOf("consumer_orgno" to orgNr)
fun getSystemBrukerClaim(orgNr: String) = mapOf(
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
