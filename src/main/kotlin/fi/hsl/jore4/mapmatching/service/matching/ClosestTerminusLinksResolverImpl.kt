package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.Constants
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.model.matching.TerminusType
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinksResult
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toPoint
import fi.hsl.jore4.mapmatching.util.InternalService
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.springframework.transaction.annotation.Transactional

@InternalService
class ClosestTerminusLinksResolverImpl(
    val linkRepository: ILinkRepository
) : IClosestTerminusLinksResolver {
    @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
    override fun findClosestInfrastructureLinksForRouteEndpoints(
        startPoint: Point<G2D>,
        endPoint: Point<G2D>,
        vehicleType: VehicleType,
        linkQueryDistance: Double,
        linkQueryLimit: Int
    ): Pair<List<SnappedPointOnLink>, List<SnappedPointOnLink>> {
        // The method findNClosestLinks returns one-based index.
        // The number of the closest links searched is limited by linkQueryLimit parameter.
        val linkSearchResults: Map<Int, SnapPointToLinksResult> =
            linkRepository.findNClosestLinks(
                listOf(startPoint, endPoint),
                vehicleType,
                linkQueryDistance,
                linkQueryLimit
            )

        fun getExceptionIfNoLinksFound(
            terminusPoint: Point<G2D>,
            isStartPoint: Boolean
        ): IllegalStateException {
            val terminusType = if (isStartPoint) TerminusType.START else TerminusType.END

            return IllegalStateException(
                "Could not find any infrastructure link within $linkQueryDistance meter distance " +
                    "from source route $terminusType point ($terminusPoint) " +
                    "while applying vehicle type constraint '$vehicleType'"
            )
        }

        val closestStartLinks: List<SnappedPointOnLink> =
            linkSearchResults[1]?.closestLinks
                ?: throw getExceptionIfNoLinksFound(startPoint, true)

        val closestEndLinks: List<SnappedPointOnLink> =
            linkSearchResults[2]?.closestLinks
                ?: throw getExceptionIfNoLinksFound(endPoint, false)

        return closestStartLinks to closestEndLinks
    }

    @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
    override fun resolveTerminusLinkSelectionParameters(
        sourceRouteGeometry: LineString<G2D>,
        sourceRoutePoints: List<RoutePoint>,
        vehicleType: VehicleType,
        terminusLinkQueryDistance: Double,
        terminusLinkQueryLimit: Int
    ): TerminusLinkSelectionParams {
        // The terminus locations are extracted from the LineString geometry of the source route
        // instead of the route point entities (mostly stop point instances) since in this context
        // we are interested in the start/end coordinates of the source route line.
        val startLocation: Point<G2D> = toPoint(sourceRouteGeometry.startPosition)
        val endLocation: Point<G2D> = toPoint(sourceRouteGeometry.endPosition)

        val sourceRouteStartPoint: SourceRouteTerminusPoint =
            when (val firstRoutePoint: RoutePoint = sourceRoutePoints.first()) {
                is RouteStopPoint ->
                    SourceRouteTerminusPoint.fromRouteStopPoint(
                        startLocation,
                        true,
                        firstRoutePoint.nationalId
                    )

                else -> SourceRouteTerminusPoint.fromRoutePoint(startLocation, true)
            }

        val sourceRouteEndPoint: SourceRouteTerminusPoint =
            when (val lastRoutePoint: RoutePoint = sourceRoutePoints.last()) {
                is RouteStopPoint ->
                    SourceRouteTerminusPoint.fromRouteStopPoint(
                        endLocation,
                        false,
                        lastRoutePoint.nationalId
                    )

                else -> SourceRouteTerminusPoint.fromRoutePoint(endLocation, false)
            }

        val (closestStartLinks: List<SnappedPointOnLink>, closestEndLinks: List<SnappedPointOnLink>) =
            findClosestInfrastructureLinksForRouteEndpoints(
                startLocation,
                endLocation,
                vehicleType,
                terminusLinkQueryDistance,
                terminusLinkQueryLimit
            )

        fun snapToTerminusNodes(pointsOnLinks: List<SnappedPointOnLink>): List<SnappedPointOnLink> =
            pointsOnLinks.map {
                // The location is snapped to terminus node if within close distance.
                it.withSnappedToTerminusNode(Constants.SNAP_TO_LINK_ENDPOINT_DISTANCE_IN_METERS)
            }

        return TerminusLinkSelectionParams(
            sourceRouteStartPoint,
            snapToTerminusNodes(closestStartLinks),
            sourceRouteEndPoint,
            snapToTerminusNodes(closestEndLinks)
        )
    }
}
