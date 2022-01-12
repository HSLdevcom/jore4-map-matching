package fi.hsl.jore4.mapmatching.test.generators

import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans

object DistanceGenerator {

    fun distancePair(source1: Gen<Double>, source2: Gen<Double>): Gen<Pair<Double, Double>> {
        // Randomise order of distances generated from different sources.
        return booleans()
            .zip(source1, source2) { flipOrder, distance1, distance2 ->
                if (flipOrder)
                    Pair(distance2, distance1)
                else
                    Pair(distance1, distance2)
            }
    }
}
