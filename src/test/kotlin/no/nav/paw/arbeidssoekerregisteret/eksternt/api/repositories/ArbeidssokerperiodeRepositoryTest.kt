package no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.domain.Arbeidssoekerperiode
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.domain.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.TimeUtils.getMaxDateForDatabaseStorage
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.toInstant
import org.jetbrains.exposed.sql.Database
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

class ArbeidssokerperiodeRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var database: Database

    beforeSpec {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent en periode" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val periode = hentTestPeriode()
        repository.opprettArbeidssoekerperiode(periode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.periodeId)

        retrievedPeriode shouldNotBe null
    }

    "Hent en periode for et gitt identitetsnummer" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val identitetsnummer = Identitetsnummer("12345678911")

        val perioder = repository.hentArbeidssoekerperioder(identitetsnummer, null)

        perioder.size shouldBeExactly 1
    }

    "Oppdater periode med avsluttet tidspunkt" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val periode = hentTestPeriode()
        repository.opprettArbeidssoekerperiode(periode)

        val updatedPeriode = periode.copy(avsluttet = LocalDateTime.now().minusDays(2))

        repository.oppdaterArbeidssoekerperiode(updatedPeriode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.periodeId)

        retrievedPeriode shouldNotBe null
        retrievedPeriode?.avsluttet shouldNotBe periode.avsluttet
    }

    "Oppdater periode uten avsluttet tidspunkt med nytt avsluttet tidspunkt" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val periode = hentTestPeriode().copy(avsluttet = null)
        repository.opprettArbeidssoekerperiode(periode)

        val updatedPeriode = periode.copy(avsluttet = LocalDateTime.now().minusDays(2))

        repository.oppdaterArbeidssoekerperiode(updatedPeriode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.periodeId)

        retrievedPeriode shouldNotBe null
        retrievedPeriode?.avsluttet shouldNotBe periode.avsluttet
    }

    "Oppdater fødselsnummer på periode" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val periode = hentTestPeriode()
        repository.opprettArbeidssoekerperiode(periode)

        val updatedPeriode = periode.copy(identitetsnummer = Identitetsnummer("12345678912"))

        repository.oppdaterArbeidssoekerperiode(updatedPeriode)

        val retrievedPeriode = repository.hentArbeidssoekerperiode(periode.periodeId)

        retrievedPeriode shouldNotBe null
        retrievedPeriode?.identitetsnummer shouldBe updatedPeriode.identitetsnummer
    }

    "Sletter perioder eldre enn 3 år pluss inneværende år" {
        val repository = ArbeidssoekerperiodeRepository(database)

        val perioder = hentGamleTestPerioder()

        perioder.forEach { repository.opprettArbeidssoekerperiode(it) }

        repository.slettDataEldreEnnDatoFraDatabase(getMaxDateForDatabaseStorage().toInstant())

        val retrievedPerioder = perioder.mapNotNull { repository.hentArbeidssoekerperiode(it.periodeId) }
        retrievedPerioder.size shouldBe 1
    }
})

fun hentTestPeriode(periodeId: UUID? = null): Arbeidssoekerperiode {
    val startetTimestamp = LocalDateTime.now().minusDays(10)
    val avsluttetTimestamp = LocalDateTime.now()
    return Arbeidssoekerperiode(
        Identitetsnummer("12345678911"),
        periodeId ?: UUID.randomUUID(),
        startetTimestamp,
        avsluttetTimestamp
    )
}

fun hentGamleTestPerioder(): List<Arbeidssoekerperiode> {
    val startet = getMaxDateForDatabaseStorage().minusDays(10)
    val avsluttet = getMaxDateForDatabaseStorage()
    return listOf(
        Arbeidssoekerperiode(
            Identitetsnummer("12345678913"),
            UUID.randomUUID(),
            startet,
            avsluttet.plusDays(1)
        ),
        Arbeidssoekerperiode(
            Identitetsnummer("12345678914"),
            UUID.randomUUID(),
            startet,
            avsluttet.minusDays(1)
        )
    )
}
