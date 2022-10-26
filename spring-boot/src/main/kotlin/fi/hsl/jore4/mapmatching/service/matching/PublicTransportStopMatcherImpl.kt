package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.repository.infrastructure.IStopRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.PublicTransportStopMatchParameters
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapStopToLinkResult
import fi.hsl.jore4.mapmatching.util.InternalService
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@InternalService
class PublicTransportStopMatcherImpl @Autowired constructor(val stopRepository: IStopRepository)
    : IPublicTransportStopMatcher {

    @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
    override fun findStopPointsByNationalIdsAndIndexByRoutePointOrdering(routePoints: List<RoutePoint>,
                                                                         maxStopLocationDeviation: Double)
        : Map<Int, SnapStopToLinkResult> {

        val fromRoutePointIndexToStopMatchParams: Map<Int, PublicTransportStopMatchParameters> =
            getMappingFromRoutePointIndexesToStopMatchParameters(routePoints)

        val snappedLinks: List<SnapStopToLinkResult> =
            stopRepository.findStopsAndSnapToInfrastructureLinks(fromRoutePointIndexToStopMatchParams.values,
                                                                 maxStopLocationDeviation)

        val fromStopNationalIdToSnappedLink: Map<Int, SnapStopToLinkResult> =
            snappedLinks.associateBy(SnapStopToLinkResult::stopNationalId)

        val fromRoutePointIndexToMatchedStopNationalId: Map<Int, Int> = fromRoutePointIndexToStopMatchParams
            .mapValues { it.value.nationalId }
            .filterValues(fromStopNationalIdToSnappedLink::containsKey)

        LOGGER.debug {
            "Matched following public transport stop points from source route points: ${
                joinToLogString(fromRoutePointIndexToMatchedStopNationalId.toSortedMap().entries) {
                    "Route point #${it.key + 1}: nationalId=${it.value}"
                }
            }"
        }

        return fromRoutePointIndexToMatchedStopNationalId.mapValues {
            fromStopNationalIdToSnappedLink[it.value]!!
        }
    }

    companion object {

        fun getMappingFromRoutePointIndexesToStopMatchParameters(routePoints: List<RoutePoint>):
            Map<Int, PublicTransportStopMatchParameters> {

            return routePoints
                .mapIndexedNotNull { index: Int, routePoint: RoutePoint ->
                    when (routePoint) {
                        is RouteStopPoint -> routePoint.nationalId?.let { nationalId ->

                            // Prefer projected location because it is expected to be closer to
                            // public transport stop location when compared to Digiroad locations.
                            val sourceLocation: Point<G2D> = routePoint.projectedLocation ?: routePoint.location

                            index to PublicTransportStopMatchParameters(nationalId, sourceLocation)
                        }

                        else -> null
                    }
                }
                .toMap()
        }
    }
}
