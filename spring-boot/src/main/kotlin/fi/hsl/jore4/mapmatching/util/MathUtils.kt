package fi.hsl.jore4.mapmatching.util

import java.math.BigDecimal
import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
object MathUtils {

    const val ZERO_THRESHOLD: Double = 0.00001

    fun compare(d1: Double, d2: Double): Int = BigDecimal(d1).compareTo(BigDecimal(d2))

    fun isZero(d: Double): Boolean = abs(d) < ZERO_THRESHOLD
}
