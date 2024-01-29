import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    application
}

val arbeidssokerregisteretVersion = "24.01.15.119-1"
val navCommonModulesVersion = "3.2023.12.12_13.53-510909d4aa1a"
val logstashVersion = "7.3"
val logbackVersion = "1.4.12"
val pawUtilsVersion = "24.01.11.9-1"
val pawPdlClientsVersion = "24.01.12.26-1"
val pawAaRegClientVersion = "24.01.12.16-1"

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema("no.nav.paw.arbeidssokerregisteret.api.schema:eksternt-api:$arbeidssokerregisteretVersion")
    implementation("no.nav.paw:aareg-client:$pawAaRegClientVersion")
    implementation("no.nav.paw:pdl-client:$pawPdlClientsVersion")

    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")

    implementation("no.nav.common:token-client:$navCommonModulesVersion")

    // Ktor client
    implementation("io.ktor:ktor-server-status-pages:${pawObservability.versions.ktor}")
    implementation("io.ktor:ktor-serialization-jackson:${pawObservability.versions.ktor}")
    implementation("io.ktor:ktor-client-okhttp-jvm:${pawObservability.versions.ktor}")
    implementation("io.ktor:ktor-client-logging-jvm:${pawObservability.versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${pawObservability.versions.ktor}")

    // Logging
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // Kafka
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
    implementation("no.nav.paw.kafka-streams:kafka-streams:$pawUtilsVersion")
    implementation("io.confluent:kafka-avro-serializer:7.5.3")
    implementation("io.confluent:kafka-streams-avro-serde:7.5.3")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("org.apache.kafka:kafka-clients:3.6.0")
    implementation("org.apache.kafka:kafka-streams:3.6.0")

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    //kotest
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")

    // Use the JUnit 5 integration.
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.profilering.StartupKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Title"] = rootProject.name
        attributes["Main-Class"] = application.mainClass.get()
    }
}