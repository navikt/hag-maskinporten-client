package no.nav.helsearbeidsgiver.maskinporten

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.Date
import java.util.UUID

data class MaskinportenClientConfig(
    val scope: String,
    val clientId: String,
    val clientJwk: String,
    val issuer: String,
    val endpoint: String
) {

    private val rsaKey: RSAKey by lazy {
        try {
            RSAKey.parse(clientJwk)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JWK format", e)
        }
    }
    private val signer: RSASSASigner by lazy {
        RSASSASigner(rsaKey.toPrivateKey())
    }
    private val header: JWSHeader by lazy {
        JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .type(JOSEObjectType.JWT)
            .build()
    }

    private fun currentTime(): Date = Date.from(Instant.now())

    private val claims: JWTClaimsSet by lazy {
        val now = currentTime()
        JWTClaimsSet.Builder()
            .issuer(clientId)
            .audience(issuer)
            .issueTime(now)
            .claim("scope", scope)
            .expirationTime(Date.from(now.toInstant().plusSeconds(60)))
            .jwtID(UUID.randomUUID().toString())
            .build()
    }

    fun getJwtAssertion(): String {
        return SignedJWT(header, claims).apply { sign(signer) }.serialize()
    }
}
