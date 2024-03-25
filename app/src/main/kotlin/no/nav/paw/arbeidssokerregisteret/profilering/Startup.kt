package no.nav.paw.arbeidssokerregisteret.profilering

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.profilering.application.APPLICATION_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.application.ApplicationConfiguration
import no.nav.paw.arbeidssokerregisteret.profilering.application.applicationTopology
import no.nav.paw.arbeidssokerregisteret.profilering.health.Health
import no.nav.paw.arbeidssokerregisteret.profilering.health.initKtor
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import no.nav.paw.arbeidssokerregisteret.profilering.utils.ApplicationInfo
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import no.nav.paw.config.kafka.streams.KafkaStreamsFactory
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serdes.LongSerde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("app")
    logger.info("Starter: {}", ApplicationInfo.id)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfiguration>(APPLICATION_CONFIG_FILE)
    val streamsConfig = KafkaStreamsFactory(applicationConfig.applicationIdSuffix, kafkaConfig)
        .withDefaultKeySerde(LongSerde::class)
        .withDefaultValueSerde(SpecificAvroSerde::class)
        .withExactlyOnce()

    val streamsBuilder = StreamsBuilder()
        .addStateStore(
            Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(applicationConfig.joiningStateStoreName),
                Serdes.UUID(),
                streamsConfig.createSpecificAvroSerde()
            )
        )

    val topology = applicationTopology(
        streamBuilder = streamsBuilder,
        personInfoTjeneste = PersonInfoTjeneste.create(),
        applicationConfiguration = applicationConfig,
        prometheusRegistry = prometheusMeterRegistry
    )

    val kafkaStreams = KafkaStreams(topology, streamsConfig.properties)

    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        logger.error("Uventet feil: $throwable", throwable)
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }

    kafkaStreams.start()

    val health = Health(kafkaStreams)

    logger.info("Applikasjon startet")
    initKtor(
        kafkaStreamsMetrics = KafkaStreamsMetrics(kafkaStreams),
        prometheusRegistry = prometheusMeterRegistry,
        health = health
    ).start(true)
    logger.info("Applikasjon stoppet")
}

