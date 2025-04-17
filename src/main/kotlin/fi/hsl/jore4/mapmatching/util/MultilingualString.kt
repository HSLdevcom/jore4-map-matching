package fi.hsl.jore4.mapmatching.util

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

class MultilingualString(
    @JsonValue val values: Map<String, String?>
) {
    override fun toString(): String = values.toString()

    companion object {
        @JsonCreator
        @JvmStatic
        fun of(mapOrNull: Map<String, String?>?): MultilingualString {
            val mapWithNonNullKeys: Map<String, String?> = mapOrNull ?: emptyMap()

            return MultilingualString(mapWithNonNullKeys)
        }
    }
}
