package fi.hsl.jore4.mapmatching.util

import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
object MathUtils {

    const val DOUBLE_TOLERANCE: Double = 0.00001

    fun Double.isWithinTolerance(other: Double) = abs(this - other) < DOUBLE_TOLERANCE
}
