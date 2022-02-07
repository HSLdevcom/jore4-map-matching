package fi.hsl.jore4.mapmatching.repository.routing

data class RouteDTO(val routeLinks: List<RouteLinkDTO>,
                    val trimmedStartLink: RouteLinkDTO?,
                    val trimmedEndLink: RouteLinkDTO?) {

    /**
     * Return route links using the trimmed versions of the first and the last
     * link. Trimming is done using the fractional locations of snapped points.
     */
    fun getRouteLinksWithTrimmedTerminus(): List<RouteLinkDTO> = when (routeLinks.size) {
        0 -> emptyList()
        1 -> listOf(getStartLinkAsTrimmedIfPresent())
        2 -> listOf(getStartLinkAsTrimmedIfPresent(), getEndLinkAsTrimmedIfPresent())
        else -> {
            listOf(getStartLinkAsTrimmedIfPresent())
                .plus(routeLinks.drop(1).dropLast(1))
                .plus(getEndLinkAsTrimmedIfPresent())
        }
    }

    private fun getStartLinkAsTrimmedIfPresent(): RouteLinkDTO = trimmedStartLink ?: routeLinks.first()

    private fun getEndLinkAsTrimmedIfPresent(): RouteLinkDTO = trimmedEndLink ?: routeLinks.last()
}
