package fi.hsl.jore4.mapmatching.service.matching.test

data class StopToStopSegmentation(val segments: List<StopToStopSegment>,
                                  val discardedRoutes: List<String>)
