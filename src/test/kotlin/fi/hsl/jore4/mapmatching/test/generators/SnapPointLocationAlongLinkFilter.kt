package fi.hsl.jore4.mapmatching.test.generators

enum class SnapPointLocationAlongLinkFilter {
    AT_START,
    AT_MIDPOINT,
    AT_END,

    CLOSE_TO_START,
    CLOSE_TO_END,

    AT_OR_CLOSE_TO_START,
    AT_OR_CLOSE_TO_END,

    NOT_AT_START,
    NOT_AT_END,

    IN_FIRST_HALF,
    IN_SECOND_HALF,
    BETWEEN_ENDPOINTS_EXCLUSIVE
}
