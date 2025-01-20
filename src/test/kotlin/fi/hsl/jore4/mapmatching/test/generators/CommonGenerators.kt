package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.test.util.Quadruple
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate
import org.quicktheories.generators.Generate.constant

object CommonGenerators {
    val ZERO_DOUBLE: Gen<Double> = constant(0.0)

    fun <T> pair(source: Gen<T>): Gen<Pair<T, T>> = source.zip(source, ::Pair)

    // pair of discrete values (non-equal within single generated tuple)
    fun <T : Any> discretePair(source: Gen<T>): Gen<Pair<T, T>> {
        return source.flatMap { value1 ->
            val getAnotherUniqueValue = Retry(source) { it != value1 }

            getAnotherUniqueValue.map { value2 -> Pair(value1, value2) }
        }
    }

    fun shuffledPair(
        source1: Gen<Double>,
        source2: Gen<Double>
    ): Gen<Pair<Double, Double>> {
        // Randomise order of distances generated from different sources.
        return Generate.booleans()
            .zip(source1, source2) { flipOrder, distance1, distance2 ->
                if (flipOrder) {
                    distance2 to distance1
                } else {
                    distance1 to distance2
                }
            }
    }

    fun <T> duplicate(source: Gen<T>): Gen<Pair<T, T>> = source.map { value -> value to value }

    // triple consisting of three discrete values (unique within single generated tuple)
    fun <T : Any> discreteTriple(source: Gen<T>): Gen<Triple<T, T, T>> {
        return discretePair(source)
            .flatMap { (value1, value2) ->
                val getAnotherUniqueValue = Retry(source) { it != value1 && it != value2 }

                getAnotherUniqueValue.map { value3 -> Triple(value1, value2, value3) }
            }
    }

    // quadruple consisting of four discrete values (unique within single generated tuple)
    fun <T : Any> discreteQuadruple(source: Gen<T>): Gen<Quadruple<T, T, T, T>> {
        return discreteTriple(source)
            .flatMap { (value1, value2, value3) ->
                val getAnotherUniqueValue = Retry(source) { it != value1 && it != value2 && it != value3 }

                getAnotherUniqueValue.map { value4 -> Quadruple(value1, value2, value3, value4) }
            }
    }
}
