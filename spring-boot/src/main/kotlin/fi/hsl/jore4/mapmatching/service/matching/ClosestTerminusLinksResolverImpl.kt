package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.TerminusType
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinksResult
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.util.InternalService
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@InternalService
class ClosestTerminusLinksResolverImpl @Autowired constructor(val linkRepository: ILinkRepository)
    : IClosestTerminusLinksResolver {

    @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
    override fun findClosestInfrastructureLinksForRouteEndpoints(startPoint: Point<G2D>,
                                                                 endPoint: Point<G2D>,
                                                                 vehicleType: VehicleType,
                                                                 linkQueryDistance: Double,
                                                                 linkQueryLimit: Int)
        : Pair<List<SnappedPointOnLink>, List<SnappedPointOnLink>> {

        // The method findNClosestLinks returns one-based index.
        // The number of the closest links searched is limited by linkQueryLimit parameter.
        val linkSearchResults: Map<Int, SnapPointToLinksResult> =
            linkRepository.findNClosestLinks(listOf(startPoint, endPoint),
                                             vehicleType,
                                             linkQueryDistance,
                                             linkQueryLimit)

        fun getExceptionIfNoLinksFound(terminusPoint: Point<G2D>, isStartPoint: Boolean): IllegalStateException {
            val terminusType = if (isStartPoint) TerminusType.START else TerminusType.END

            return IllegalStateException(
                "Could not find any infrastructure link within $linkQueryDistance meter distance from source route " +
                    "$terminusType point ($terminusPoint) while applying vehicle type constraint '$vehicleType'")
        }

        val closestStartLinks: List<SnappedPointOnLink> = linkSearchResults[1]?.closestLinks
            ?: throw getExceptionIfNoLinksFound(startPoint, true)

        val closestEndLinks: List<SnappedPointOnLink> = linkSearchResults[2]?.closestLinks
            ?: throw getExceptionIfNoLinksFound(endPoint, false)

        return closestStartLinks to closestEndLinks
    }
}
