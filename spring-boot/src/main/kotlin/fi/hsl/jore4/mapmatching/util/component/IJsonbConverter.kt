package fi.hsl.jore4.mapmatching.util.component

import org.jooq.JSONB

interface IJsonbConverter {

    fun asJson(obj: Any?): JSONB

    fun <T> fromJson(json: JSONB, clazz: Class<T>): T
}
