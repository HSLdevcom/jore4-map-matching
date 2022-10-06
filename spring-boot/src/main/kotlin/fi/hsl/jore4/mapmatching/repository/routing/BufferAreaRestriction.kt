package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

/**
 * Container for data that is used to geometrically restrict the target set of
 * infrastructure links in map-matching. The given line geometry is expanded in
 * all directions by the amount given by [bufferRadiusInMeters] parameter. As a
 * result, a polygon is created. The infrastructure links that are contained
 * inside the polygon area are eligible to be used as components of the
 * resulting route in map-matching. Because terminus links may lie partly
 * outside the buffer area, they can be referenced explicitly by means of
 * [explicitLinkReferences].
 *
 * @property lineGeometry the LineString geometry to be expanded
 * @property bufferRadiusInMeters the distance with which the LineString
 * geometry is expanded in all directions
 * @property explicitLinkReferences contains set of terminus link and node
 * identifiers with which terminus links can be explicitly referenced.
 */
data class BufferAreaRestriction(val lineGeometry: LineString<G2D>,
                                 val bufferRadiusInMeters: Double,
                                 val explicitLinkReferences: ExplicitLinkReferences? = null) {

    /**
     * @property idsOfCandidatesForTerminusLinks the list of identifiers for
     * candidate terminus links on route. The terminus links may lie partly outside
     * the buffer area. In such cases, they may be referenced explicitly by their
     * identifiers.
     * @property idsOfCandidatesForTerminusNodes the list of identifiers for
     * candidate terminus nodes on route. The terminus links may lie partly outside
     * the buffer area. In such cases, they may be referenced explicitly by the
     * identifiers of their endpoint nodes.
     */
    data class ExplicitLinkReferences(val idsOfCandidatesForTerminusLinks: Set<InfrastructureLinkId>,
                                      val idsOfCandidatesForTerminusNodes: Set<InfrastructureNodeId>) {

        companion object {

            fun fromTerminusLinks(startLink: SnappedLinkState, endLink: SnappedLinkState): ExplicitLinkReferences {
                val snappedNodeOnStartLink: InfrastructureNodeId? = startLink.findSnappedNode()
                val snappedNodeOnEndLink: InfrastructureNodeId? = endLink.findSnappedNode()

                val idsOfCandidatesForTerminusLinks: Set<InfrastructureLinkId>
                val idsOfCandidatesForTerminusNodes: Set<InfrastructureNodeId>

                if (snappedNodeOnStartLink != null) {
                    if (snappedNodeOnEndLink != null) {
                        idsOfCandidatesForTerminusLinks = emptySet()
                        idsOfCandidatesForTerminusNodes = setOf(snappedNodeOnStartLink, snappedNodeOnEndLink)
                    } else {
                        idsOfCandidatesForTerminusLinks = setOf(endLink.infrastructureLinkId)
                        idsOfCandidatesForTerminusNodes = setOf(snappedNodeOnStartLink)
                    }
                } else if (snappedNodeOnEndLink != null) {
                    idsOfCandidatesForTerminusLinks = setOf(startLink.infrastructureLinkId)
                    idsOfCandidatesForTerminusNodes = setOf(snappedNodeOnEndLink)
                } else {
                    idsOfCandidatesForTerminusLinks = setOf(startLink.infrastructureLinkId,
                                                            endLink.infrastructureLinkId)
                    idsOfCandidatesForTerminusNodes = emptySet()
                }

                return ExplicitLinkReferences(idsOfCandidatesForTerminusLinks,
                                              idsOfCandidatesForTerminusNodes)
            }

            fun fromTerminusPoints(startPoint: PgRoutingPoint, endPoint: PgRoutingPoint): ExplicitLinkReferences {

                val idsOfCandidatesForTerminusLinks: MutableSet<InfrastructureLinkId> = HashSet(2)
                val idsOfCandidatesForTerminusNodes: MutableSet<InfrastructureNodeId> = HashSet(2)

                fun addPoint(point: PgRoutingPoint) {
                    when (point) {
                        is RealNode -> idsOfCandidatesForTerminusNodes.add(point.nodeId)
                        is VirtualNode -> idsOfCandidatesForTerminusLinks.add(point.linkId)
                    }
                }

                addPoint(startPoint)
                addPoint(endPoint)

                return ExplicitLinkReferences(idsOfCandidatesForTerminusLinks, idsOfCandidatesForTerminusNodes)
            }
        }
    }

    companion object {

        fun from(lineGeometry: LineString<G2D>,
                 bufferRadiusInMeters: Double,
                 startLink: SnappedLinkState,
                 endLink: SnappedLinkState) = BufferAreaRestriction(lineGeometry,
                                                                    bufferRadiusInMeters,
                                                                    ExplicitLinkReferences.fromTerminusLinks(startLink,
                                                                                                             endLink))

        fun from(lineGeometry: LineString<G2D>,
                 bufferRadiusInMeters: Double,
                 startPoint: PgRoutingPoint,
                 endPoint: PgRoutingPoint) = BufferAreaRestriction(lineGeometry,
                                                                   bufferRadiusInMeters,
                                                                   ExplicitLinkReferences.fromTerminusPoints(
                                                                       startPoint,
                                                                       endPoint))
    }
}
