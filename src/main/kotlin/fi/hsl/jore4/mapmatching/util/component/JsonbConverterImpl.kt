package fi.hsl.jore4.mapmatching.util.component

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.JSONB
import org.springframework.stereotype.Component

@Component
class JsonbConverterImpl(
    val objectMapper: ObjectMapper
) : IJsonbConverter {
    override fun asJson(obj: Any?): JSONB =
        try {
            JSONB.jsonb(objectMapper.writeValueAsString(obj))
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }

    override fun <T> fromJson(
        json: JSONB,
        clazz: Class<T>
    ): T =
        try {
            objectMapper.readValue(json.data(), clazz)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
}
