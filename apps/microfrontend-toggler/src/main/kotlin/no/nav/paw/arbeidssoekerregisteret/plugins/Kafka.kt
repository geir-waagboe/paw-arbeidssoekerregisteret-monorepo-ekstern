package no.nav.paw.arbeidssoekerregisteret.plugins


import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.arbeidssoekerregisteret.config.HealthIndicator
import no.nav.paw.arbeidssoekerregisteret.config.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.plugins.kafka.KafkaStreamsPlugin
import no.nav.paw.arbeidssoekerregisteret.service.HealthIndicatorService
import no.nav.paw.arbeidssoekerregisteret.topology.buildPeriodeTopology
import no.nav.paw.arbeidssoekerregisteret.topology.buildSiste14aVedtakTopology
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler

val logger = buildApplicationLogger

fun Application.configureKafka(
    appConfig: AppConfig,
    healthIndicatorService: HealthIndicatorService,
    meterRegistry: MeterRegistry,
    hentKafkaKeys: (ident: String) -> KafkaKeysResponse?
): List<KafkaStreamsMetrics> {

    logger.info("Oppretter Kafka Stream for arbeidssøkerperioder")
    val periodeKafkaStreams = buildKafkaStreams(
        appConfig,
        appConfig.kafkaStreams.periodeStreamIdSuffix,
        buildPeriodeTopology(appConfig, meterRegistry, hentKafkaKeys),
        buildStateListener(
            healthIndicatorService.newLivenessIndicator(),
            healthIndicatorService.newReadinessIndicator()
        )
    )
    logger.info("Oppretter Kafka Stream for 14a-vedtak")
    val siste14aVedtakKafkaStreams = buildKafkaStreams(
        appConfig,
        appConfig.kafkaStreams.siste14aVedtakStreamIdSuffix,
        buildSiste14aVedtakTopology(appConfig, meterRegistry, hentKafkaKeys),
        buildStateListener(
            healthIndicatorService.newLivenessIndicator(),
            healthIndicatorService.newReadinessIndicator()
        )
    )

    val kafkaStreamsList = mutableListOf(periodeKafkaStreams, siste14aVedtakKafkaStreams)

    install(KafkaStreamsPlugin) {
        kafkaStreamsConfig = appConfig.kafkaStreams
        kafkaStreams = kafkaStreamsList
    }

    return kafkaStreamsList.map { KafkaStreamsMetrics(it) }
}

private fun buildKafkaStreams(
    appConfig: AppConfig,
    applicationIdSuffix: String,
    topology: Topology,
    stateListener: KafkaStreams.StateListener
): KafkaStreams {
    val streamsFactory = KafkaStreamsFactory(applicationIdSuffix, appConfig.kafka)
        .withDefaultKeySerde(Serdes.Long()::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)

    val kafkaStreams = KafkaStreams(
        topology,
        StreamsConfig(streamsFactory.properties)
    )
    kafkaStreams.setStateListener(stateListener)
    kafkaStreams.setUncaughtExceptionHandler(buildUncaughtExceptionHandler())
    return kafkaStreams
}

private fun buildStateListener(
    livenessHealthIndicator: HealthIndicator,
    readinessHealthIndicator: HealthIndicator
) = KafkaStreams.StateListener { newState, _ ->
    when (newState) {
        KafkaStreams.State.RUNNING -> {
            readinessHealthIndicator.setHealthy()
        }

        KafkaStreams.State.REBALANCING -> {
            readinessHealthIndicator.setHealthy()
        }

        KafkaStreams.State.PENDING_ERROR -> {
            readinessHealthIndicator.setUnhealthy()
        }

        KafkaStreams.State.PENDING_SHUTDOWN -> {
            readinessHealthIndicator.setUnhealthy()
        }

        KafkaStreams.State.ERROR -> {
            readinessHealthIndicator.setUnhealthy()
            livenessHealthIndicator.setUnhealthy()
        }

        else -> {
            readinessHealthIndicator.setUnknown()
        }
    }

    logger.info("Kafka Streams liveness er ${livenessHealthIndicator.getStatus().value}")
    logger.info("Kafka Streams readiness er ${readinessHealthIndicator.getStatus().value}")
}

private fun buildUncaughtExceptionHandler() = StreamsUncaughtExceptionHandler { throwable ->
    logger.error("Kafka Streams opplevde en uventet feil", throwable)
    StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
}
