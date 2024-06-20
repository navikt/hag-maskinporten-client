package no.nav.helsearbeidsgiver.maskinporten

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.*

class MaskinportenClientConfig(scope: String) {

    private val clientId: String = EnvWrapper.getEnv("MASKINPORTEN_CLIENT_ID") ?: throw IllegalStateException("Fant ikke MASKINPORTEN_CLIENT_ID")
    private val clientJwk: String = EnvWrapper.getEnv("MASKINPORTEN_CLIENT_JWK") ?: throw IllegalStateException("Fant ikke MASKINPORTEN_CLIENT_JWK")
    private val issuer: String = EnvWrapper.getEnv("MASKINPORTEN_ISSUER") ?: throw IllegalStateException("Fant ikke MASKINPORTEN_ISSUER")
    // Denne er foreløig ikke i bruk, men kan verifisere at scopet er riktig ved et senere tidspunkt.
    private val scopes: String = EnvWrapper.getEnv("MASKINPORTEN_SCOPES") ?: throw IllegalStateException("Fant ikke MASKINPORTEN_SCOPES")
    private val ENDPOINT: String = EnvWrapper.getEnv("MASKINPORTEN_TOKEN_ENDPOINT") ?: throw IllegalStateException("Fant ikke MASKINPORTEN_TOKEN_ENDPOINT")

    private val rsaKey: RSAKey = RSAKey.parse(clientJwk)
    private val signer: RSASSASigner = RSASSASigner(rsaKey.toPrivateKey())
    private val header: JWSHeader = JWSHeader.Builder(JWSAlgorithm.RS256)
        .keyID(rsaKey.keyID)
        .type(JOSEObjectType.JWT)
        .build()

    private val now: Date = Date.from(Instant.now())
    private val expiration: Date = Date.from(Instant.now().plusSeconds(60))
    private val claims: JWTClaimsSet = JWTClaimsSet.Builder()
        .issuer(clientId)
        .audience(issuer)
        .issueTime(now)
        .claim("scope", scope)
        .expirationTime(expiration)
        .jwtID(UUID.randomUUID().toString())
        .build()

    fun getJwtAssertion(): String = SignedJWT(header, claims)
        .apply { sign(signer) }
        .serialize()

    fun getEndpoint(): String {
        return ENDPOINT
    }

}

object EnvWrapper {
    fun getEnv(key: String): String? {
        return System.getenv(key)
    }
}