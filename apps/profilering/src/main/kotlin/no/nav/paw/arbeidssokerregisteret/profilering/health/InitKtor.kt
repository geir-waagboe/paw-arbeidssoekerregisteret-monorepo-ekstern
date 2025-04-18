package no.nav.paw.arbeidssokerregisteret.profilering.health

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun initKtor(
    kafkaStreamsMetrics: KafkaStreamsMetrics,
    prometheusRegistry: PrometheusMeterRegistry,
    health: Health
) = embeddedServer(Netty, port = 8080) {
    install(MicrometerMetrics) {
        registry = prometheusRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            kafkaStreamsMetrics
        )
    }
    routing {
        get("/isReady") {
            val status = health.ready()
            call.respond(status.code, status.message)
        }
        get("/isAlive") {
            val alive = health.alive()
            call.respond(alive.code, alive.message)
        }
        get("/metrics") {
            call.respond(prometheusRegistry.scrape())
        }
    }
}
