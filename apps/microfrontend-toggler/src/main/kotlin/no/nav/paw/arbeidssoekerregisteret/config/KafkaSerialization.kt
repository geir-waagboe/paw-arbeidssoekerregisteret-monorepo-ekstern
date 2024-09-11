package no.nav.paw.arbeidssoekerregisteret.config

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.arbeidssoekerregisteret.model.Beriket14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtak
import no.nav.paw.arbeidssoekerregisteret.model.Siste14aVedtakInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.env.currentNaisEnv
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

inline fun <reified T> buildJsonSerializer(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Serializer<T> {
    override fun serialize(topic: String?, data: T): ByteArray {
        if (data == null) return byteArrayOf()
        try {
            return objectMapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            if (naisEnv == NaisEnv.ProdGCP && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonDeserializer(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Deserializer<T> {
    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        try {
            return objectMapper.readValue<T>(data)
        } catch (e: Exception) {
            if (naisEnv == NaisEnv.ProdGCP && e is JsonProcessingException) e.clearLocation()
            throw e
        }
    }
}

inline fun <reified T> buildJsonSerde(naisEnv: NaisEnv, objectMapper: ObjectMapper) = object : Serde<T> {
    override fun serializer(): Serializer<T> {
        return buildJsonSerializer(naisEnv, objectMapper)
    }

    override fun deserializer(): Deserializer<T> {
        return buildJsonDeserializer(naisEnv, objectMapper)
    }
}

inline fun <reified T> buildJsonSerde(): Serde<T> {
    return buildJsonSerde<T>(currentNaisEnv, buildObjectMapper)
}

fun buildPeriodeInfoSerde() = buildJsonSerde<PeriodeInfo>()

fun buildSiste14aVedtakSerde() = buildJsonSerde<Siste14aVedtak>()

fun buildSiste14aVedtakInfoSerde() = buildJsonSerde<Siste14aVedtakInfo>()

fun buildBeriket14aVedtakSerde() = buildJsonSerde<Beriket14aVedtak>()

fun buildToggleSerde() = buildJsonSerde<Toggle>()

class ToggleJsonSerializer(private val delegate: Serializer<Toggle>) : Serializer<Toggle> {
    constructor() : this(buildJsonSerializer(currentNaisEnv, buildObjectMapper))

    override fun serialize(topic: String?, data: Toggle?): ByteArray {
        return delegate.serialize(topic, data)
    }
}
