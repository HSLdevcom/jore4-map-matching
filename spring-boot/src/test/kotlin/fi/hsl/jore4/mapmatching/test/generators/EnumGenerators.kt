package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.enumValues

object EnumGenerators {

    fun trafficFlowDirectionType(): Gen<TrafficFlowDirectionType> = enumValues(TrafficFlowDirectionType::class.java)

    fun locationAlongLinkType(): Gen<SnapPointLocationAlongLinkFilter> = enumValues(SnapPointLocationAlongLinkFilter::class.java)
}
