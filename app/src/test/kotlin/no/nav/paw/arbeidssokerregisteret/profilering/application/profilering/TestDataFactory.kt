package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v3.Utdanning
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as ApiMetadata
import java.time.Instant
import java.util.*


fun periode(
    id: UUID = UUID.randomUUID(),
    identitetsnummer: String = "12345678911",
    startet: ApiMetadata = metadata(),
    avsluttet: ApiMetadata? = null
) = Periode(id, identitetsnummer, startet, avsluttet)

fun opplysninger(
    id: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    sendtInAv: Metadata = metadata(),
    utdanning: Utdanning = utdanning(),
    helse: Helse = helse(),
    arbeidserfaring: Arbeidserfaring = arbeidserfaring(),
    jobbsituasjon: Jobbsituasjon = jobbsituasjon(),
    annet: Annet = annet()
) = OpplysningerOmArbeidssoeker(
    id,
    periodeId,
    sendtInAv,
    utdanning,
    helse,
    arbeidserfaring,
    jobbsituasjon,
    annet
)

fun utdanning(
    utdanning: String = "3",
    utdanningGodkjent: JaNeiVetIkke = JaNeiVetIkke.JA,
    utdanningBestatt: JaNeiVetIkke = JaNeiVetIkke.JA
) = Utdanning(utdanning, utdanningGodkjent, utdanningBestatt)

fun helse(
    helseHinder: JaNeiVetIkke = JaNeiVetIkke.NEI
) = Helse(helseHinder)

fun arbeidserfaring(
    arbeidserfaring: JaNeiVetIkke = JaNeiVetIkke.JA
) = Arbeidserfaring(arbeidserfaring)

fun jobbsituasjon(
    jobbsituasjon: List<BeskrivelseMedDetaljer> = emptyList()
) = Jobbsituasjon(jobbsituasjon)

fun annet(
    annet: JaNeiVetIkke = JaNeiVetIkke.NEI
) = Annet(annet)

fun metadata(
    tidspunkt: Instant = Instant.now(),
    bruker: Bruker = bruker()
) = ApiMetadata(
    tidspunkt,
    bruker,
    "1",
    "2"
)

fun bruker(
    type: BrukerType = BrukerType.SYSTEM,
    id: String = "test"
) = Bruker(type, id)