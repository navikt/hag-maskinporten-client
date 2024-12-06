package no.nav.helsearbeidsgiver.maskinporten

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import no.nav.helsearbeidsgiver.utils.log.logger
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

interface MaskinportenClientConfig {
    val scope: String
    val issuer: String
    val aud: String
    val endpoint: String
    fun getJwtAssertion(): String
}

/**
 * MaskinportenClientConfigPkey er en implementasjon av MaskinportenClientConfig med privatekey som autentiseringsmetode for maskinporten
 *
 * @param kid  Det er id-en til Nøkkelen key-id (kid)
 * @param privateKey  Det er privatekey som skal brukes til å signere JWT tokenet
 * @param issuer  Det er Klient-id eller integration-id fra maskinporten det er en UUID
 * @param aud  Det er mottaker av tokenet i test er det https://test-maskinporten.no/
 * @param consumerOrgNr  Det er organisasjonsnummeret til virksomheten som skal bruke maskinporten på vegne av
 * @param scope  Det er rettighetene som skal gis til maskinporten
 * @param endpoint  Det er endepunktet til maskinporten
 *
 */
class MaskinportenClientConfigPkey(
    val kid: String,
    val privateKey: String,
    override val issuer: String,
    override val aud: String,
    val consumerOrgNr: String,
    override val scope: String,
    override val endpoint: String ,
) : MaskinportenClientConfig {

    private fun loadPrivateKey(key: String): PrivateKey {
        val keyText =
            key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\n", "")
                .replace("\\s".toRegex(), "")

        val encoded = Base64.getDecoder().decode(keyText)
        return KeyFactory
            .getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(encoded))
    }

    override fun getJwtAssertion(): String {
        val currentTimestamp = System.currentTimeMillis() / 1000

        val header =
            JWSHeader
                .Builder(JWSAlgorithm.RS256)
                .keyID(kid)
                .type(JOSEObjectType.JWT)
                .build()

        val claims =
            JWTClaimsSet
                .Builder()
                .issuer(issuer)
                .audience(aud)
                .issueTime(Date(currentTimestamp * 1000))
                .expirationTime(Date((currentTimestamp + 60) * 1000))
                .claim("scope", scope)
                .claim("consumer_org", consumerOrgNr)
                .jwtID(UUID.randomUUID().toString())
                .build()

        val signer = RSASSASigner(loadPrivateKey(privateKey))
        val signedJWT = SignedJWT(header, claims)
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }
}

/**
 * MaskinportenSimpleAssertion er en implementasjon av MaskinportenClientConfig med assertion som autentiseringsmetode
 * for maskinporten Denne brukes for å authentisere mot maskinporten for eksample for Altinn
 *
 * @param scope  Det er rettighetene som skal gis til maskinporten
 * @param issuer  Det er Klient-id eller integration-id fra maskinporten det er en UUID
 * @param aud  Det er mottaker av tokenet i test er det https://test-maskinporten.no/
 * @param endpoint  Det er endepunktet til maskinporten
 * @param clientJwk  Det er JWK som skal brukes til å signere JWT tokenet
 *
 */
class MaskinportenSimpleAssertion(
    override val scope: String,
    override val issuer: String,
    override val aud: String,
    override val endpoint: String,
    val clientJwk: String,
) : MaskinportenClientConfig {

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

    private fun claims(): JWTClaimsSet {
        val now = currentTime()
        return JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(aud)
            .issueTime(now)
            .claim("scope", scope)
            .expirationTime(Date.from(now.toInstant().plusSeconds(60)))
            .jwtID(UUID.randomUUID().toString())
            .build()
    }

    override fun getJwtAssertion(): String {
        return SignedJWT(header, claims()).apply { sign(signer) }.serialize()
    }
}
