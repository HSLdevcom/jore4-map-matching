package fi.hsl.jore4.mapmatching.util

import java.math.BigDecimal

object MathUtils {

    fun compare(d1: Double, d2: Double): Int = BigDecimal(d1).compareTo(BigDecimal(d2))
}
