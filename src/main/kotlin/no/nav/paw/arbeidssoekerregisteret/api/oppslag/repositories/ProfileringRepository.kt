package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toMetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfilertTilResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ProfileringRepository(private val database: Database) {
    fun hentProfileringForArbeidssoekerMedPeriodeId(periodeId: UUID): List<ProfileringResponse> =
        transaction(database) {
            ProfileringTable.selectAll().where { ProfileringTable.periodeId eq periodeId }.map { resultRow ->
                ProfileringConverter(this@ProfileringRepository).konverterTilProfileringResponse(resultRow)
            }
        }

    fun hentProfileringForArbeidssoekerMedIdentitetsnummer(identitetsnummer: Identitetsnummer): List<ProfileringResponse> =
        transaction(database) {
            val periodeIder = ArbeidssoekerperiodeRepository(database).hentArbeidssoekerperioder(identitetsnummer).map { it.periodeId }

            periodeIder.flatMap { periodeId ->
                ProfileringTable.selectAll().where { ProfileringTable.periodeId eq periodeId }.map { resultRow ->
                    ProfileringConverter(this@ProfileringRepository).konverterTilProfileringResponse(resultRow)
                }
            }
        }

    /*fun hentProfileringForArbeidssoekerMedIdentitetsnummer(
        identitetsnummer: Identitetsnummer,
        limit: Int? = null
    ): List<ProfileringResponse> =
        transaction(database) {
            ProfileringTable
                .innerJoin(MetadataTable, { sendtInnAvId }, { MetadataTable.id })
                .innerJoin(PeriodeOpplysningerTable, { OpplysningerOmArbeidssoekerTable.id }, { opplysningerOmArbeidssoekerTableId })
                .innerJoin(PeriodeTable, { PeriodeOpplysningerTable.periodeId }, { periodeId })
                .selectAll().where { PeriodeTable.identitetsnummer eq identitetsnummer.verdi }
                .orderBy(MetadataTable.tidspunkt, SortOrder.DESC)
                .let { query ->
                    if (limit != null) query.limit(limit) else query
                }
                .mapNotNull { resultRow ->
                    ProfileringConverter(this@ProfileringRepository).konverterTilProfileringResponse(resultRow)
                }
        }*/

    fun storeBatch(batch: Sequence<Profilering>) =
        transaction(database) {
            repetitionAttempts = 2
            minRepetitionDelay = 20
            batch.forEach { profilering ->
                opprettProfileringForArbeidssoeker(profilering)
            }
        }

    fun opprettProfileringForArbeidssoeker(profilering: Profilering) =
        transaction(database) {
            repetitionAttempts = 2
            minRepetitionDelay = 20
            val sendtInnAvId = ArbeidssoekerperiodeRepository(database).settInnMetadata(profilering.sendtInnAv)
            ProfileringTable.insert {
                it[profileringId] = profilering.id
                it[periodeId] = profilering.periodeId
                it[opplysningerOmArbeidssoekerId] = profilering.opplysningerOmArbeidssokerId
                it[ProfileringTable.sendtInnAvId] = sendtInnAvId
                it[profilertTil] = profilering.profilertTil
                it[jobbetSammenhengendeSeksAvTolvSisteManeder] = profilering.jobbetSammenhengendeSeksAvTolvSisteMnd
                it[alder] = profilering.alder
            }
        }

    fun hentMetadata(metadataId: Long) = ArbeidssoekerperiodeRepository(database).hentMetadata(metadataId)
}

class ProfileringConverter(private val profileringRepository: ProfileringRepository) {
    fun konverterTilProfileringResponse(resultRow: ResultRow): ProfileringResponse {
        println("resultRow: $resultRow")
        val profileringId = resultRow[ProfileringTable.profileringId]
        val periodeId = resultRow[ProfileringTable.periodeId]
        val opplysningerOmArbeidssoekerId = resultRow[ProfileringTable.opplysningerOmArbeidssoekerId]
        val sendtInnAvId = resultRow[ProfileringTable.sendtInnAvId]
        val profilertTil = resultRow[ProfileringTable.profilertTil]
        val jobbetSammenhengendeSeksAvTolvSisteManeder = resultRow[ProfileringTable.jobbetSammenhengendeSeksAvTolvSisteManeder]
        val alder = resultRow[ProfileringTable.alder]

        val sendtInnAv = profileringRepository.hentMetadata(sendtInnAvId)?.toMetadataResponse() ?: throw Error("Fant ikke metadata")

        return ProfileringResponse(
            profileringId,
            periodeId,
            opplysningerOmArbeidssoekerId,
            sendtInnAv,
            profilertTil.toProfilertTilResponse(),
            jobbetSammenhengendeSeksAvTolvSisteManeder,
            alder
        )
    }
}
