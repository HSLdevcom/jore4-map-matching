package fi.hsl.jore4.mapmatching.model

/**
 * Indicates which side of an infrastructure link is affected with regard to the LineString that
 * models the geometry of the link in question. This is mainly used to express which side of an
 * infrastructure link is affected by a public transport stop associated with it.
 */
enum class LinkSide {
    LEFT,
    RIGHT,
    BOTH
}
