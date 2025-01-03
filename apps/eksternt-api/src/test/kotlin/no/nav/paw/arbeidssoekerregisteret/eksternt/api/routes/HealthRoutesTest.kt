package no.nav.paw.arbeidssoekerregisteret.eksternt.api.routes

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class HealthRoutesTest : FreeSpec({
    "Skal svare mee 200 OK" {
        testApplication {
            environment { config = MapApplicationConfig() }
            routing {
                healthRoutes(PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
            }

            val isAliveResponse = client.get("/internal/isAlive")
            isAliveResponse.status shouldBe HttpStatusCode.OK

            val isReadyResponse = client.get("/internal/isReady")
            isReadyResponse.status shouldBe HttpStatusCode.OK

            val metricsResponse = client.get("/internal/metrics")
            metricsResponse.status shouldBe HttpStatusCode.OK
        }
    }
})
