package fi.hsl.jore4.mapmatching.util

import fi.hsl.jore4.mapmatching.util.MathUtils.isWithinTolerance
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.core.Gen
import org.quicktheories.generators.SourceDSL.doubles
import org.quicktheories.generators.SourceDSL.integers
import java.math.BigDecimal

class MathUtilsTest {
    private data class ToleranceTestTriple(
        val tolerance: Double,
        val arbitraryNumber: Double,
        val deltaWithinOrOutsideTolerance: Double
    )

    @Nested
    @DisplayName("isWithinTolerance")
    inner class IsWithinTolerance {
        @Nested
        @DisplayName("Test start of nominal range [0-1]")
        inner class TestStartOfNominalRange {
            private val tolerance: Double = 0.00001

            @Test
            fun whenExpectedToBeOutsideTolerance() {
                assertThat(tolerance.isWithinTolerance(0.0, tolerance)).isEqualTo(false)
            }

            @Test
            fun whenExpectedToBeWithinTolerance() {
                assertThat(0.000009.isWithinTolerance(0.0, tolerance)).isEqualTo(true)
            }
        }

        @Nested
        @DisplayName("Test end of nominal range [0-1]")
        inner class TestEndOfNominalRange {
            private val tolerance: Double = 0.00001

            @Test
            fun whenExpectedToBeOutsideTolerance() {
                assertThat(0.99999.isWithinTolerance(1.0, tolerance)).isEqualTo(false)
            }

            @Test
            fun whenExpectedToBeWithinTolerance() {
                assertThat(0.999991.isWithinTolerance(1.0, tolerance)).isEqualTo(true)
            }
        }

        @Nested
        @DisplayName("Test using large set of generated values")
        inner class GenerativeTests {
            private val genTolerance: Gen<Double> =
                integers().between(-2, 10).map { decimalPrecision ->
                    BigDecimal.ONE.movePointLeft(decimalPrecision).toDouble()
                }

            @Test
            @DisplayName("With generated numbers within tolerance")
            fun withGeneratedNumbersWithinTolerance() {
                val genTriple: Gen<ToleranceTestTriple> =
                    genTolerance.flatMap { tolerance ->
                        genFiniteDouble(tolerance).flatMap { arbitraryNumber: Double ->

                            // Generate delta as Double whose absolute value is less than given tolerance.
                            doubles()
                                .between(-0.99999 * tolerance, 0.99999 * tolerance)
                                .map { delta: Double ->
                                    ToleranceTestTriple(tolerance, arbitraryNumber, delta)
                                }
                        }
                    }

                qt()
                    .forAll(genTriple)
                    .checkAssert { (tolerance, number, deltaWithinTolerance) ->

                        val numWithinTolerance: Double = plusOrMinusResultingFinite(number, deltaWithinTolerance)

                        assertThat(numWithinTolerance.isWithinTolerance(number, tolerance))
                            .isEqualTo(true)
                    }
            }

            @Test
            @DisplayName("With generated numbers outside tolerance")
            fun withGeneratedNumbersOutsideTolerance() {
                val genTriple: Gen<ToleranceTestTriple> =
                    genTolerance.flatMap { tolerance ->
                        genFiniteDouble(tolerance).flatMap { arbitraryNumber: Double ->

                            // Generate delta as Double whose absolute value is greater than or equal to given
                            // tolerance.
                            doubles().between(tolerance, maxDouble(tolerance)).map { delta: Double ->
                                ToleranceTestTriple(tolerance, arbitraryNumber, delta)
                            }
                        }
                    }

                qt()
                    .forAll(genTriple)
                    .checkAssert { (tolerance, number, deltaOutsideTolerance) ->

                        val numOutsideTolerance: Double = plusOrMinusResultingFinite(number, deltaOutsideTolerance)

                        assertThat(numOutsideTolerance.isWithinTolerance(number, tolerance))
                            .isEqualTo(false)
                    }
            }
        }
    }

    companion object {
        private fun genFiniteDouble(tolerance: Double): Gen<Double> =
            doubles().between(
                minDouble(tolerance),
                maxDouble(tolerance)
            )

        private fun minDouble(tolerance: Double): Double = Double.MIN_VALUE + tolerance

        private fun maxDouble(tolerance: Double): Double = Double.MAX_VALUE - tolerance

        private fun plusOrMinusResultingFinite(
            n1: Double,
            n2: Double
        ): Double {
            val addition: Double = n1 + n2

            return if (java.lang.Double.isFinite(addition)) {
                addition
            } else {
                n1 - n2
            }
        }
    }
}
