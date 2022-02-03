package fi.hsl.jore4.mapmatching.util

import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
object MathUtils {

    const val ZERO_THRESHOLD: Double = 0.00001

    fun isZero(d: Double): Boolean = abs(d) < ZERO_THRESHOLD

    fun isZeroOrNegative(d: Double): Boolean = d < ZERO_THRESHOLD

    /**
     * Clamp [Double] to zero if it is negative or below zero threshold.
     */
    fun clampToZero(d: Double): Double = if (isZeroOrNegative(d)) 0.0 else d
}
