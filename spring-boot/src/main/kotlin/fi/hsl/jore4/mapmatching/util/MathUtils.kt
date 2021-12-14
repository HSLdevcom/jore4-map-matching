package fi.hsl.jore4.mapmatching.util

import java.math.BigDecimal

object MathUtils {

    fun compare(decimal1: Double, decimal2: Double): Int = BigDecimal(decimal1).compareTo(BigDecimal(decimal2))
}
