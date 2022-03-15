package fi.hsl.jore4.mapmatching.repository.infrastructure

interface IStopRepository {

    /**
     * Find public transport stops constrained by collection of matching parameters and snap the
     * matched stops to infrastructure links.
     *
     * @param stopMatchParams collection of parameters that are used to match route stop points with
     * the public transport stops hosted by this map-matching service.
     * @param maxDistanceBetweenExpectedAndActualStopLocation the maximum distance within which two
     * locations given for a public transport stop are allowed to be away from each other, in order
     * to include the stop in the set of route points that are matched with infrastructure links.
     * The first location is the one hosted in this map-matching service (mostly originating from
     * Digiroad) and the second one is the location defined within the client system (invoking this
     * map-matching service). If the distance between these two type of locations exceeds
     * [maxDistanceBetweenExpectedAndActualStopLocation], then the affected stops are discarded from
     * the set of route points that are matched with infrastructure links.
     *
     * @return [List] of [SnapStopToLinkDTO]s found by the given national identifiers.
     */
    fun findStopsAndSnapToInfrastructureLinks(stopMatchParams: Collection<PublicTransportStopMatchParameters>,
                                              maxDistanceBetweenExpectedAndActualStopLocation: Double)
        : List<SnapStopToLinkDTO>
}
