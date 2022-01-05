package fi.hsl.jore4.mapmatching.util

object CollectionUtils {
    fun <T> filterOutConsecutiveDuplicates(coll: Collection<out T>): List<T> {
        val list = mutableListOf<T>()
        var prev: T? = null

        coll.forEach {
            if (it != prev) {
                list.add(it)
                prev = it
            }
        }

        return list
    }
}
