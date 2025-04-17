package fi.hsl.jore4.mapmatching.model.matching

enum class TerminusType {
    START,
    END;

    override fun toString(): String = name.lowercase()
}
