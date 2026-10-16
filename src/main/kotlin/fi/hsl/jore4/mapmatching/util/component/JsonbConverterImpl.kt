package fi.hsl.jore4.mapmatching.util.component

import org.jooq.JSONB
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class JsonbConverterImpl(
    val jsonMapper: JsonMapper
) : IJsonbConverter {
    override fun asJson(obj: Any?): JSONB = JSONB.jsonb(jsonMapper.writeValueAsString(obj))

    override fun <T> fromJson(
        json: JSONB,
        clazz: Class<T>
    ): T = jsonMapper.readValue(json.data(), clazz)
}
