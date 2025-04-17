package fi.hsl.jore4.mapmatching.util

import java.math.BigDecimal
import java.math.MathContext

object MathUtils {
    const val DEFAULT_DOUBLE_TOLERANCE = 0.00001

    private const val MAX_DECIMAL_PRECISION_FOR_TOLERANCE_CHECKING = 10

    private val MIN_DOUBLE_TOLERANCE = BigDecimal.ONE.movePointLeft(MAX_DECIMAL_PRECISION_FOR_TOLERANCE_CHECKING)

    fun Double.isWithinTolerance(other: Double): Boolean = isWithinTolerance(other, DEFAULT_DOUBLE_TOLERANCE)

    fun Double.isWithinTolerance(
        other: Double,
        tolerance: Double
    ): Boolean {
        val refinedTolerance = bigDecimalForToleranceChecking(tolerance)

        require(refinedTolerance >= MIN_DOUBLE_TOLERANCE) { "tolerance must be >= $MIN_DOUBLE_TOLERANCE" }

        val difference: BigDecimal = bigDecimalForToleranceChecking(this) - bigDecimalForToleranceChecking(other)

        return difference.abs() < refinedTolerance
    }

    private fun bigDecimalForToleranceChecking(n: Double): BigDecimal =
        BigDecimal(n, MathContext(MAX_DECIMAL_PRECISION_FOR_TOLERANCE_CHECKING))
}
