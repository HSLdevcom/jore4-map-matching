package fi.hsl.jore4.mapmatching.repository.routing

data class RouteDTO(val routeLinks: List<RouteLinkDTO>,
                    val trimmedStartLink: RouteLinkDTO?,
                    val trimmedEndLink: RouteLinkDTO?) {

    /**
     * Return route links using the trimmed versions of the first and the last
     * link. Trimming is done using the fractional locations of snapped points.
     */
    fun getRouteLinksWithTrimmedTermini(): List<RouteLinkDTO> = when (routeLinks.size) {
        0 -> emptyList()
        1 -> listOf(trimmedStartLink ?: routeLinks.first())
        else -> when {
            trimmedStartLink == null && trimmedEndLink == null -> routeLinks
            else -> {
                val start: List<RouteLinkDTO> = listOf(trimmedStartLink ?: routeLinks.first())
                val end: List<RouteLinkDTO> = listOf(trimmedEndLink ?: routeLinks.last())

                start + routeLinks.drop(1).dropLast(1) + end
            }
        }
    }

    companion object {
        val EMPTY = RouteDTO(emptyList(), null, null)
    }
}
