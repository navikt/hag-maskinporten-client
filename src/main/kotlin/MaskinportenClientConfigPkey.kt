package no.nav.helsearbeidsgiver.maskinporten

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date
import java.util.UUID

/**
 * MaskinportenClientConfigPkey er en implementasjon av MaskinportenClientConfig med privatekey som autentiseringsmetode for maskinporten
 *
 * @param kid  Det er id-en til Nøkkelen key-id (kid)
 * @param privateKey  Det er privatekey som skal brukes til å signere JWT tokenet
 * @param clientId  issuer - Din egen client_id.
 * @param issuer Audience - issuer-identifikatoren til Maskinporten. Verdi for aktuelt miljø finner du på .well-known-endpunkt.
 * @param consumerOrgNr  Det er organisasjonsnummeret til virksomheten som skal bruke maskinporten på vegne av
 * @param scope Space-separert liste over scopes som klienten forespør.
 * @param endpoint  Det er endepunktet til maskinporten
 *
 */
class MaskinportenClientConfigPkey(
    val kid: String,
    val privateKey: String,
    override val clientId: String,
    override val issuer: String,
    override val scope: String,
    override val endpoint: String,
    override val additionalClaims: Map<String, Any>? = null
) : MaskinportenClientConfig {

    val currentTimestamp = System.currentTimeMillis() / 1000
    override fun getJwtAssertion(): String {
        val header =
            JWSHeader
                .Builder(JWSAlgorithm.RS256)
                .keyID(kid)
                .type(JOSEObjectType.JWT)
                .build()

        val signer = RSASSASigner(loadPrivateKey(privateKey))
        val signedJWT = SignedJWT(header, claims())
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }

    private fun claims(): JWTClaimsSet {
        val now = currentTime()
        val builder = JWTClaimsSet
            .Builder()
            .issuer(clientId)
            .audience(issuer)
            .issueTime(now)
            .expirationTime(Date.from(now.toInstant().plusSeconds(60)))
            .claim("scope", scope)
            .jwtID(UUID.randomUUID().toString())

        additionalClaims?.forEach { (key, value) ->
            builder.claim(key, value)
        }

        return builder.build()
    }
}

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
