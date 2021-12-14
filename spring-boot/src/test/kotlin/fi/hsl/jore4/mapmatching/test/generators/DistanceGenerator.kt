package fi.hsl.jore4.mapmatching.test.generators

import org.quicktheories.core.Gen

object DistanceGenerator {

    fun distancePair(source1: Gen<Double>, source2: Gen<Double>): Gen<Pair<Double, Double>> =
        // Randomise order of distances generated from different sources.
        CommonGenerators.BOOLEAN
            .zip(source1, source2) { flipOrder, distance1, distance2 ->
                if (flipOrder)
                    Pair(distance2, distance1)
                else
                    Pair(distance1, distance2)
            }
}
