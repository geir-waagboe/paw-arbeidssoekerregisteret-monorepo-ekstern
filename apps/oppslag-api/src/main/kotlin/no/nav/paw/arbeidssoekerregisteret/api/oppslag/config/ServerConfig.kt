package no.nav.paw.arbeidssoekerregisteret.api.oppslag.config

import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment

const val SERVER_CONFIG = "server_config.toml"

data class ServerConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val gracePeriodMillis: Long = 300,
    val timeoutMillis: Long = 300,
    val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
)
