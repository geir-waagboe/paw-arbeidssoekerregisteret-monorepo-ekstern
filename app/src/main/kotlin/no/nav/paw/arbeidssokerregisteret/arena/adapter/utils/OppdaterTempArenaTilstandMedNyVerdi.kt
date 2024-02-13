package no.nav.paw.arbeidssokerregisteret.arena.adapter.utils

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.toArena
import no.nav.paw.arbeidssokerregisteret.arena.v1.Annet
import no.nav.paw.arbeidssokerregisteret.arena.v1.Arbeidserfaring
import no.nav.paw.arbeidssokerregisteret.arena.v1.Helse
import no.nav.paw.arbeidssokerregisteret.arena.v3.TempArenaArbeidssokerregisterTilstand
import org.apache.avro.specific.SpecificRecord

fun oppdaterTempArenaTilstandMedNyVerdi(
    nyVerdi: SpecificRecord?,
    gjeldeneTilstand: TempArenaArbeidssokerregisterTilstand
): TempArenaArbeidssokerregisterTilstand {
    val periode = (nyVerdi as? Periode)
        ?.let(::toArena) ?: gjeldeneTilstand.periode
    val profilering = (nyVerdi as? Profilering)
        ?.let(::toArena) ?: gjeldeneTilstand.profilering
    val opplysninger = (nyVerdi as? OpplysningerOmArbeidssoeker)
        ?.let(::toArena) ?: gjeldeneTilstand.opplysningerOmArbeidssoeker
    return TempArenaArbeidssokerregisterTilstand(
        periode,
        profilering,
        opplysninger
    )
}

private fun toArena(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker) =
    no.nav.paw.arbeidssokerregisteret.arena.v3.OpplysningerOmArbeidssoeker(
        opplysningerOmArbeidssoeker.id,
        opplysningerOmArbeidssoeker.periodeId,
        opplysningerOmArbeidssoeker.sendtInnAv.toArena(),
        opplysningerOmArbeidssoeker.utdanning.toArena(),
        Helse(opplysningerOmArbeidssoeker.helse.helsetilstandHindrerArbeid.toArena()),
        Arbeidserfaring(opplysningerOmArbeidssoeker.arbeidserfaring.harHattArbeid.toArena()),
        opplysningerOmArbeidssoeker.jobbsituasjon.toArena(),
        Annet(opplysningerOmArbeidssoeker.annet.andreForholdHindrerArbeid.toArena())
    )

private fun toArena(profilering: Profilering) =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Profilering(
        profilering.id,
        profilering.periodeId,
        profilering.opplysningerOmArbeidssokerId,
        profilering.sendtInnAv.toArena(),
        profilering.profilertTil.toArena(),
        profilering.jobbetSammenhengendeSeksAvTolvSisteMnd,
        profilering.alder
    )

private fun toArena(periode: Periode) =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Periode(
        periode.id,
        periode.identitetsnummer,
        periode.startet.toArena(),
        periode.avsluttet?.toArena()
    )
