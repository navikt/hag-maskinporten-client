package no.nav.helsearbeidsgiver.maskinporten

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helsearbeidsgiver.utils.log.logger


private const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer"

class MaskinportenClient(scope: String) {

    private var httpClient = createHttpClient()
    private var maskinportenClientConfig = MaskinportenClientConfig(scope)
    private var logger = this.logger()


    suspend fun fetchNewAccessToken(): TokenResponseWrapper {
        logger.info("Henter ny access token fra Maskinporten")

        val result = runCatching {
            val response: HttpResponse = httpClient.post(maskinportenClientConfig.getEndpoint()) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    listOf(
                        "grant_type" to GRANT_TYPE,
                        "assertion" to maskinportenClientConfig.getJwtAssertion()
                    ).formUrlEncode()
                )
            }
            response.body<TokenResponse>()
        }
        return result.fold(
            onSuccess = { tokenResponse ->
                TokenResponseWrapper(tokenResponse).also {
                    logger.info("Hentet ny access token. Expires in ${it.remainingTimeInSeconds} seconds.")
                }
            },
            onFailure = { e ->
                when (e) {
                    is ClientRequestException -> {
                        logger.error("ClientRequestException: Feilet å hente ny access token fra Maskinporten. Status: ${e.response.status}, Message: ${e.message} Exception: $e")
                    }

                    is ServerResponseException -> {
                        logger.error("ServerResponseException: Feilet å hente ny access token fra Maskinporten. Status: ${e.response.status}, Message: ${e.message} Exception: $e")
                    }

                    else -> {
                        logger.error("Feilet å hente ny access token fra Maskinporten: $e")
                    }
                }
                throw e
            }
        )
    }

    fun setHttpClient(httpClient: HttpClient) {
        this.httpClient = httpClient
    }
}





