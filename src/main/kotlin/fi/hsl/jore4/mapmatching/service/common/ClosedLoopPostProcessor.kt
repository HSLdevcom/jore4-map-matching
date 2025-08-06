package fi.hsl.jore4.mapmatching.service.common

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.repository.routing.RouteLink

object ClosedLoopPostProcessor {
    /**
     * One full traversal replaces consecutive traversals (full, partial,
     * reversed, reversed partial) on a closed-loop shaped infrastructure link
     * in the direction of the first traversal appearing per loop. The handling
     * is applied for all appearances of closed loops in a route. This is a kind
     * of compatibility mode for Jore4 where route granularity is defined in
     * terms of whole infrastructure link geometries. Therefore, we may want to
     * prevent inadvertent multi-traversals in closed loops.
     */
    fun simplifyConsecutiveClosedLoopTraversals(routeLinks: List<RouteLink>): List<RouteLink> {
        val modifiedRouteLinks: MutableList<RouteLink> = ArrayList(routeLinks.size)

        var lastAddedTraversal: InfrastructureLinkTraversal? = null

        routeLinks.forEach { routeLink ->
            val linkTraversal = routeLink.linkTraversal

            lastAddedTraversal
                ?.let { lastAdded: InfrastructureLinkTraversal ->

                    if (linkTraversal.isClosedLoop &&
                        lastAdded.isClosedLoop &&
                        linkTraversal.infrastructureLinkId == lastAdded.infrastructureLinkId
                    ) {
                        // Let's skip current link traversal and modify the last added traversal.

                        val (routeSeqNum: Int) = modifiedRouteLinks.removeLast()

                        val replacementLinkTraversal =
                            InfrastructureLinkTraversal(
                                lastAdded.infrastructureLinkId,
                                lastAdded.externalLinkRef,
                                lastAdded.linkGeometry,
                                lastAdded.linkGeometry, // traversedGeometry is replaced by linkGeometry
                                lastAdded.isTraversalForwards,
                                lastAdded.linkLength,
                                lastAdded.linkLength, // traversedDistance is replaced by linkLength
                                true,
                                lastAdded.infrastructureLinkName
                            )

                        modifiedRouteLinks.add(RouteLink(routeSeqNum, replacementLinkTraversal))
                        lastAddedTraversal = replacementLinkTraversal
                    } else {
                        modifiedRouteLinks.add(routeLink)
                        lastAddedTraversal = linkTraversal
                    }
                }
                ?: run {
                    modifiedRouteLinks.add(routeLink)
                    lastAddedTraversal = linkTraversal
                }
        }

        return modifiedRouteLinks
    }
}
