package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.END_NODE_CLOSER
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.END_NODE_CLOSER_OR_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.NODES_AT_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.START_NODE_CLOSER
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.START_NODE_CLOSER_OR_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import fi.hsl.jore4.mapmatching.util.MathUtils
import org.quicktheories.core.Gen
import org.quicktheories.generators.SourceDSL.doubles

object DistanceToNodeGenerator {

    // Random distances from arbitrary point to either endpoint of the closest link
    fun positiveDistance(): Gen<Double> = doubles().between(0.5, 500.0)

    // mix 5% zeros
    fun nonNegativeDistance(): Gen<Double> = positiveDistance().mix(CommonGenerators.ZERO_DOUBLE, 5)

    // Generate pairs of node distances e.g. from arbitrary point to endpoints of single infrastructure link.
    fun nodeDistancePair(): Gen<Pair<Double, Double>> =
        DistanceGenerator.distancePair(nonNegativeDistance(), positiveDistance())

    fun nodeDistancePair(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<Pair<Double, Double>> {
        return when (nodeProximityFilter) {
            // Make a duplicate from a generated value.
            NODES_AT_EQUAL_DISTANCE -> positiveDistance().map { distance: Double ->
                Pair(distance, distance)
            }
            else -> nodeDistancePair()
                .assuming { (firstDistance: Double, secondDistance: Double) ->
                    when (nodeProximityFilter) {
                        // filter out pairs of equal value
                        START_NODE_CLOSER, END_NODE_CLOSER -> MathUtils.compare(firstDistance, secondDistance) != 0
                        else -> true
                    }
                }
                .map { (distance1: Double, distance2: Double) ->

                    val firstDistanceLessOrEqual: Boolean = when (MathUtils.compare(distance1, distance2)) {
                        1 -> false
                        else -> true
                    }

                    when (nodeProximityFilter) {
                        START_NODE_CLOSER, START_NODE_CLOSER_OR_EQUAL_DISTANCE -> {
                            if (firstDistanceLessOrEqual)
                                Pair(distance1, distance2)
                            else
                                Pair(distance2, distance1)
                        }
                        END_NODE_CLOSER, END_NODE_CLOSER_OR_EQUAL_DISTANCE -> {
                            if (firstDistanceLessOrEqual)
                                Pair(distance2, distance1)
                            else
                                Pair(distance1, distance2)
                        }
                        else -> throw IllegalStateException("should not end up here")
                    }
                }
        }
    }

    fun nodeDistanceTriple(): Gen<Triple<Double, Double, Double>> {
        return nodeDistancePair()
            .zip(positiveDistance()) { (distance1, distance2), distance3 ->
                Triple(distance1, distance2, distance3)
            }
    }

    fun nodeDistanceQuadruple(): Gen<Quadruple<Double, Double, Double, Double>> {
        return nodeDistanceTriple()
            .zip(positiveDistance()) { (distance1, distance2, distance3), distance4 ->
                Quadruple(distance1, distance2, distance3, distance4)
            }
    }
}
