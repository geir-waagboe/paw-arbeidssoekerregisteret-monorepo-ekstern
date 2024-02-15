package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneId.systemDefault())

fun LocalDateTime.toInstant(): Instant = this.atZone(ZoneId.systemDefault()).toInstant()

fun getDeletionInterval(): Long = 1000L * 60 * 60 * 24 // 24 timer

object TimeUtils {
    private val now = LocalDateTime.now()

    private fun getStartOfYear(): LocalDateTime = now.with(TemporalAdjusters.firstDayOfYear()).withHour(0).withMinute(0).withSecond(0).withNano(0)

    private fun getDurationFromNowToStartOfYear(): Duration = Duration.between(now, getStartOfYear())

    // Maks lagring for data er inneværende år pluss 3 år
    fun getMaxDateForDatabaseStorage(): LocalDateTime = now.minus(getDurationFromNowToStartOfYear()).minusYears(3)
}
