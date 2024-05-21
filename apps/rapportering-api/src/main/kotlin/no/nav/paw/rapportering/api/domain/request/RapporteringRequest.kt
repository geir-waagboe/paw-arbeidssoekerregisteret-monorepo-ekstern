package no.nav.paw.rapportering.api.domain.request

import java.util.*

data class RapporteringRequest(
    // Identitetsnummer må sendes med hvis det er en veileder som rapporterer
    val identitetsnummer: String? = null,
    val periodeId: UUID,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)