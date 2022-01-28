package fi.hsl.jore4.mapmatching.util

import kotlin.math.abs

@Suppress("MemberVisibilityCanBePrivate")
object MathUtils {

    const val ZERO_THRESHOLD: Double = 0.00001

    fun isZero(d: Double): Boolean = abs(d) < ZERO_THRESHOLD
}
