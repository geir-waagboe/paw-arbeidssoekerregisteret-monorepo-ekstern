package no.nav.paw.security.authentication.model

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.principal
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.authentication.token.resolveTokens
import no.nav.paw.security.authorization.exception.SecurityContextManglerException
import no.nav.paw.security.authorization.exception.UgyldigBearerTokenException
import no.nav.paw.security.authorization.exception.UgyldigBrukerException
import no.nav.security.token.support.v2.TokenValidationContextPrincipal
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("no.nav.paw.logger.security.authentication")

data class SecurityContext(
    val bruker: Bruker<*>,
    val accessToken: AccessToken
) : Principal

fun ApplicationCall.resolveSecurityContext(): SecurityContext {
    val principal = principal<TokenValidationContextPrincipal>()
    val tokenContext = principal?.context
        ?: throw UgyldigBearerTokenException("Ugyldig eller manglende Bearer Token")

    val accessToken = tokenContext.resolveTokens().firstOrNull() // Kan støtte flere tokens
        ?: throw UgyldigBearerTokenException("Ingen gyldige Bearer Tokens funnet")

    val bruker = when (accessToken.issuer) {
        is TokenX -> {
            logger.debug("TokenX token -> Sluttbruker")
            Sluttbruker(accessToken.claims.getOrThrow(PID))
        }

        is AzureAd -> {
            if (accessToken.isM2MToken()) {
                val navIdentHeader = request.headers[NavIdentHeader.name]
                if (navIdentHeader.isNullOrBlank()) {
                    logger.debug("AzureAd M2M token -> M2MToken")
                    M2MToken(accessToken.claims.getOrThrow(OID))
                } else {
                    logger.debug("AzureAd M2M token -> NavAnsatt")
                    NavAnsatt(accessToken.claims.getOrThrow(OID), navIdentHeader)
                }
            } else {
                logger.debug("AzureAd token -> NavAnsatt")
                NavAnsatt(accessToken.claims.getOrThrow(OID), accessToken.claims.getOrThrow(NavIdent))
            }
        }

        is IdPorten -> {
            logger.debug("IdPorten token -> Sluttbruker")
            Sluttbruker(accessToken.claims.getOrThrow(PID))
        }

        is MaskinPorten -> {
            logger.debug("MaskinPorten token -> M2MToken")
            M2MToken(UUID.randomUUID())
        }
    }

    return SecurityContext(
        bruker = bruker,
        accessToken = accessToken
    )
}

fun ApplicationCall.securityContext(): SecurityContext {
    return authentication.principal<SecurityContext>()
        ?: throw SecurityContextManglerException("Finner ikke security context principal")
}

fun ApplicationCall.securityContext(securityContext: SecurityContext) {
    authentication.principal(securityContext)
}

inline fun <reified T : Bruker<*>> SecurityContext.resolveBruker(): T {
    when (bruker) {
        is T -> return bruker
        else -> throw UgyldigBrukerException("Bruker er ikke av forventet type")
    }
}

inline fun <reified T : Bruker<*>> ApplicationCall.bruker(): T {
    return securityContext().resolveBruker()
}
