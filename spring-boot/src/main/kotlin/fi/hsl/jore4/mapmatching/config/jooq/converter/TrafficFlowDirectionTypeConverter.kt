package fi.hsl.jore4.mapmatching.config.jooq.converter

import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import org.jooq.impl.AbstractConverter

class TrafficFlowDirectionTypeConverter
    : AbstractConverter<Int, TrafficFlowDirectionType>(Int::class.java, TrafficFlowDirectionType::class.java) {

    override fun from(dbValue: Int) = TrafficFlowDirectionType.from(dbValue)

    override fun to(trafficFlowDirectionType: TrafficFlowDirectionType): Int = trafficFlowDirectionType.dbValue
}
