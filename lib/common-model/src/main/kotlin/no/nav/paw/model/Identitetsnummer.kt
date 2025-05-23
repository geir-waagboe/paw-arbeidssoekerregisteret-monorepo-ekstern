package no.nav.paw.model

@JvmInline
value class Identitetsnummer(val verdi: String) {
    override fun toString(): String {
        return "*".repeat(verdi.length)
    }
}

fun String.asIdentitetsnummer(): Identitetsnummer = Identitetsnummer(this)
