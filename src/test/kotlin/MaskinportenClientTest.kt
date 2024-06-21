
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.maskinporten.EnvWrapper
import no.nav.helsearbeidsgiver.maskinporten.MaskinportenClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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

        val client = httpclientMock(mockEngine)

        val maskinportenClient = MaskinportenClient("test_scope")
        maskinportenClient.setHttpClient(client)

        val tokenResponseWrapper = maskinportenClient.fetchNewAccessToken()

        assertEquals("test_token", tokenResponseWrapper.tokenResponse.accessToken)
        assertEquals(3600, tokenResponseWrapper.tokenResponse.expiresInSeconds)
        assertEquals("test:test1", tokenResponseWrapper.tokenResponse.scope)
        assertEquals("Bearer", tokenResponseWrapper.tokenResponse.tokenType)
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

        val client = httpclientMock(mockEngine)

        val maskinportenClient = MaskinportenClient("test_scope")
        maskinportenClient.setHttpClient(client)

        val exception = assertFailsWith<ClientRequestException> { maskinportenClient.fetchNewAccessToken() }

        assertEquals(HttpStatusCode.Unauthorized.value, exception.response.status.value)
    }

    private fun httpclientMock(mockEngine: MockEngine) = HttpClient(mockEngine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json()
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setUp() {
            mockkObject(EnvWrapper)
            every { EnvWrapper.getEnv("MASKINPORTEN_CLIENT_ID") } returns "TEST_CLIENT_ID"
            every { EnvWrapper.getEnv("MASKINPORTEN_CLIENT_JWK") } returns generateJWK()
            every { EnvWrapper.getEnv("MASKINPORTEN_ISSUER") } returns "https://test.test.no/"
            every { EnvWrapper.getEnv("MASKINPORTEN_SCOPES") } returns "test:test/test"
            every { EnvWrapper.getEnv("MASKINPORTEN_TOKEN_ENDPOINT") } returns "https://test.test.no/token"
        }

        private fun generateJWK() = RSAKeyGenerator(2048).keyID("test-key-id").generate().toString()

        @JvmStatic
        @AfterAll
        fun tearDown() {
            unmockkAll()
        }
    }
}