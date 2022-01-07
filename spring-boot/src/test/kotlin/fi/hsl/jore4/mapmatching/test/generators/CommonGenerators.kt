package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.test.util.Quadruple
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.Generate.constant

object CommonGenerators {

    val BOOLEAN: Gen<Boolean> = booleans()

    val ZERO_DOUBLE: Gen<Double> = constant(0.0)

    // pair values are distinct (not equal)
    fun <T> distinctPair(source: Gen<T>): Gen<Pair<T, T>> {
        return source.flatMap { value1 ->
            val getAnotherUniqueValue = Retry(source) { it != value1 }

            getAnotherUniqueValue.map { value2 -> Pair(value1, value2) }
        }
    }

    // triple consists of three distinct values (unique within single generated tuple)
    fun <T> distinctTriple(source: Gen<T>): Gen<Triple<T, T, T>> {
        return distinctPair(source)
            .flatMap { (value1, value2) ->
                val getAnotherUniqueValue = Retry(source) { it != value1 && it != value2 }

                getAnotherUniqueValue.map { value3 -> Triple(value1, value2, value3) }
            }
    }

    // quadruple consists of four distinct values (unique within single generated tuple)
    fun <T> distinctQuadruple(source: Gen<T>): Gen<Quadruple<T, T, T, T>> {
        return distinctTriple(source)
            .flatMap { (value1, value2, value3) ->
                val getAnotherUniqueValue = Retry(source) { it != value1 && it != value2 && it != value3 }

                getAnotherUniqueValue.map { value4 -> Quadruple(value1, value2, value3, value4) }
            }
    }
}
