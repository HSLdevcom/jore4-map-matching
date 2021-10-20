package fi.hsl.jore4.mapmatching.util

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

class MultilingualString(@JsonValue val values: Map<String, String?>) {

    override fun toString(): String {
        return values.toString()
    }

    companion object {
        @JsonCreator
        @JvmStatic
        fun of(mapOrNull: Map<String?, String?>?): MultilingualString {
            val mapWithNonNullKeys: Map<String, String?> = mapOrNull?.let { map ->
                map.filterKeys { it != null } as Map<String, String?>
            } ?: emptyMap()

            return MultilingualString(mapWithNonNullKeys)
        }
    }
}
