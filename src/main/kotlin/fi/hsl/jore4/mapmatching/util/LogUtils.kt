package fi.hsl.jore4.mapmatching.util

object LogUtils {
    /**
     * Transforms a sequence of items to a String in a log-friendly form. Provides a consistent
     * way of displaying collection of items in a log message.
     */
    fun <T> joinToLogString(
        iterable: Iterable<T>,
        transform: ((T) -> CharSequence)? = null
    ) = iterable.joinToString(prefix = "[", postfix = "\n]") { item ->
        "\n  ${transform?.let { it(item) } ?: item.toString()}"
    }
}
