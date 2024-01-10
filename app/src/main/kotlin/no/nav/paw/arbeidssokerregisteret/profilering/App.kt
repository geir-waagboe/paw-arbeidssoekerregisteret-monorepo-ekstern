package no.nav.paw.arbeidssokerregisteret.profilering

import no.nav.paw.aareg.AaregClient
import no.nav.paw.arbeidssokerregisteret.profilering.aareg.AAREG_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.aareg.AaRegClientConfig
import no.nav.paw.arbeidssokerregisteret.profilering.authentication.AZURE_CONFIG_FILE
import no.nav.paw.arbeidssokerregisteret.profilering.authentication.m2mTokenFactory
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.config.kafka.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.config.kafka.KafkaConfig
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("main")
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val m2mTokenFactory = m2mTokenFactory(loadNaisOrLocalConfiguration(AZURE_CONFIG_FILE))
    val aaRegClient = with (loadNaisOrLocalConfiguration<AaRegClientConfig>(AAREG_CONFIG_FILE)) {
        AaregClient(url) { m2mTokenFactory.create(scope) }
    }
}

object ApplicationInfo {
    private val pkg = this::class.java.`package`
    val version: String? = pkg.implementationVersion
    val name: String? = pkg.implementationTitle
}
