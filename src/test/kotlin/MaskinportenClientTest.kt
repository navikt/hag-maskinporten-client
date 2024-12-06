import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.maskinporten.MaskinportenClient
import no.nav.helsearbeidsgiver.maskinporten.MaskinportenClientConfigPkey
import no.nav.helsearbeidsgiver.maskinporten.MaskinportenClientConfigSimpleAssertion
import no.nav.helsearbeidsgiver.maskinporten.createHttpClient
import no.nav.helsearbeidsgiver.utils.test.mock.mockStatic
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MaskinportenClientTest {

    @Test
    fun testFetchNewAccessTokenSuccess() = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"access_token":"test_token","token_type" : "Bearer","expires_in":3600,"scope" : "test:test1"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            )
        }
        mockStatic(::createHttpClient) {
            every { createHttpClient() } returns httpclientMock(mockEngine)
            val maskinportenClient = MaskinportenClient(
                getMaskinportenClientConfig()
            )

            val tokenResponseWrapper = maskinportenClient.fetchNewAccessToken()

            assertEquals("test_token", tokenResponseWrapper.tokenResponse.accessToken)
            assertEquals(3600, tokenResponseWrapper.tokenResponse.expiresInSeconds)
            assertEquals("test:test1", tokenResponseWrapper.tokenResponse.scope)
            assertEquals("Bearer", tokenResponseWrapper.tokenResponse.tokenType)
        }
    }

    @Test
    fun testFetchNewAccessTokenFailure() = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "Unauthorized",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf("Content-Type" to listOf(ContentType.Text.Plain.toString()))
            )
        }

        mockStatic(::createHttpClient) {
            every { createHttpClient() } returns httpclientMock(mockEngine)
            val maskinportenClient = MaskinportenClient(getMaskinportenClientConfig())

            val exception = assertFailsWith<ClientRequestException> { maskinportenClient.fetchNewAccessToken() }

            assertEquals(HttpStatusCode.Unauthorized.value, exception.response.status.value)
        }
    }

    @Test
    fun testFetchNewAccesTokenMaskinpotenPkey(): Unit = runBlocking {
        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"access_token":"test_token","token_type" : "Bearer","expires_in":3600,"scope" : "test:test1"}""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
            )
        }
        mockStatic(::createHttpClient) {
            every { createHttpClient() } returns httpclientMock(mockEngine)
            val maskinportenClient =
                MaskinportenClient(
                    MaskinportenClientConfigPkey(
                        kid = "test_kid",
                        privateKey = generatePkey(),
                        issuer = "test_issuer",
                        consumerOrgNr = "test_consumer_org_nr",
                        scope = "test_scope",
                        aud = "https://test.test.no/",
                        endpoint = "https://test.test.no/token"
                    )
                )
            val tokenResponseWrapper = maskinportenClient.fetchNewAccessToken()

            assertEquals("test_token", tokenResponseWrapper.tokenResponse.accessToken)
            assertEquals(3600, tokenResponseWrapper.tokenResponse.expiresInSeconds)
            assertEquals("test:test1", tokenResponseWrapper.tokenResponse.scope)
            assertEquals("Bearer", tokenResponseWrapper.tokenResponse.tokenType)
        }
    }

    private fun getMaskinportenClientConfig() = MaskinportenClientConfigSimpleAssertion(
        scope = "test_scope",
        issuer = "test_client_id",
        clientJwk = generateJWK(),
        endpoint = "https://test.test.no/",
        aud = "https://test.test.no/token"
    )

    private fun httpclientMock(mockEngine: MockEngine) = HttpClient(mockEngine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json()
        }
    }

    private fun generateJWK() = RSAKeyGenerator(2048).generate().toJSONString()
    private fun generatePkey(): String {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        val keyPair = keyGen.generateKeyPair()

        val pkey = Base64.getEncoder().encodeToString(keyPair.private.encoded)
        return "-----BEGIN PRIVATE KEY-----\n$pkey\n-----END PRIVATE KEY-----"
    }
}
