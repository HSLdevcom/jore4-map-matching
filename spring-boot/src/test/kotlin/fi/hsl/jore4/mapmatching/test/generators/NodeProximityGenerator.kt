package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.infrastructureNodeId
import org.quicktheories.core.Gen
import org.quicktheories.generators.SourceDSL

object NodeProximityGenerator {

    // Random distances from arbitrary point to either endpoint of the closest link
    private fun positiveDistance(): Gen<Double> = SourceDSL.doubles().between(0.5, 500.0)

    // mix 5% zeros
    private fun nonNegativeDistance(): Gen<Double> = positiveDistance().mix(CommonGenerators.ZERO_DOUBLE, 5)

    fun node(): Gen<NodeProximity> = infrastructureNodeId().zip(nonNegativeDistance(), ::NodeProximity)

    fun node(id: InfrastructureNodeId): Gen<NodeProximity> = nonNegativeDistance().map { distanceToNode ->
        NodeProximity(id, distanceToNode)
    }
}
