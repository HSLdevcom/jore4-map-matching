package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence

/**
 * Models a candidate to be tested while resolving an optimal sequence of
 * infrastructure network node identifiers and terminus links while finding
 * route through infrastructure network.
 *
 * @property startLinkId the identifier for candidate infrastructure link from
 * which route could start
 * @property endLinkId the identifier for candidate infrastructure link at which
 * route could end
 * @property nodeIdSequence sequence of infrastructure network node identifiers
 * related to a referenced start and end link.
 */
data class NodeSequenceCandidate(
    val startLinkId: InfrastructureLinkId,
    val endLinkId: InfrastructureLinkId,
    val nodeIdSequence: NodeIdSequence
)
