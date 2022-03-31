package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.INodeRepository
import fi.hsl.jore4.mapmatching.repository.routing.NodeSequenceCandidate
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Component
class NodeServiceInternalImpl @Autowired constructor(val nodeRepository: INodeRepository) : INodeServiceInternal {

    @Transactional(readOnly = true, noRollbackFor = [IllegalStateException::class])
    override fun resolveNodeIdSequence(nodeSequenceCandidates: List<NodeSequenceCandidatesBetweenSnappedLinks>,
                                       vehicleType: VehicleType,
                                       bufferAreaRestriction: BufferAreaRestriction?)
        : NodeSequenceResolutionResult {

        fun getDebugLogMessageForNodeSequenceCombinations(nodeIdSequences: List<NodeIdSequence>) =
            "Resolving best node identifier sequence from combinations: ${joinToLogString(nodeIdSequences)}"

        if (nodeSequenceCandidates.size == 1) {
            val candidatesBetweenLinks: NodeSequenceCandidatesBetweenSnappedLinks = nodeSequenceCandidates.first()
            val nodeIdSequences: List<NodeIdSequence> = candidatesBetweenLinks.nodeIdSequences

            if (nodeIdSequences.size == 1) {
                return NodeSequenceResolutionSucceeded(nodeIdSequences.first(),
                                                       candidatesBetweenLinks.startLink,
                                                       candidatesBetweenLinks.endLink)
            }

            LOGGER.debug {
                getDebugLogMessageForNodeSequenceCombinations(nodeIdSequences)
            }

        } else {
            LOGGER.debug {
                val nodeIdSequences: List<NodeIdSequence> = nodeSequenceCandidates.flatMap { it.nodeIdSequences }

                getDebugLogMessageForNodeSequenceCombinations(nodeIdSequences)
            }
        }

        fun flattenCandidates(candidates: NodeSequenceCandidatesBetweenSnappedLinks): List<NodeSequenceCandidate> {
            return candidates.nodeIdSequences.map { nodeIdSequence ->
                NodeSequenceCandidate(candidates.startLink.infrastructureLinkId,
                                      candidates.endLink.infrastructureLinkId,
                                      nodeIdSequence)
            }
        }

        val possibleNodeSequences: Map<Pair<InfrastructureLinkId, InfrastructureLinkId>, NodeIdSequence>? =
            nodeSequenceCandidates.firstNotNullOfOrNull { candidatesBetweenTwoLinks ->

                val bufferAreaRestrictionWithTerminusLinkIds: BufferAreaRestriction? =
                    bufferAreaRestriction?.run {
                        BufferAreaRestriction.from(lineGeometry,
                                                   bufferRadiusInMeters,
                                                   candidatesBetweenTwoLinks.startLink,
                                                   candidatesBetweenTwoLinks.endLink)
                    }

                nodeRepository
                    .resolveBestNodeSequences(flattenCandidates(candidatesBetweenTwoLinks),
                                              vehicleType,
                                              bufferAreaRestrictionWithTerminusLinkIds)
                    .takeIf { it.isNotEmpty() }
            }

        if (possibleNodeSequences == null) {
            val flattenedNodeSequenceCandidates: List<NodeSequenceCandidate> =
                nodeSequenceCandidates.flatMap { flattenCandidates(it) }

            return NodeSequenceResolutionFailed(
                "Could not resolve node identifier sequence from ${joinToLogString(flattenedNodeSequenceCandidates)}"
            )
        }

        return nodeSequenceCandidates
            .firstNotNullOfOrNull { candidatesBetweenTwoLinks ->
                val startLink: SnappedLinkState = candidatesBetweenTwoLinks.startLink
                val endLink: SnappedLinkState = candidatesBetweenTwoLinks.endLink

                val nodeSeqKey: Pair<InfrastructureLinkId, InfrastructureLinkId> =
                    startLink.infrastructureLinkId to endLink.infrastructureLinkId

                possibleNodeSequences[nodeSeqKey]
                    ?.let { nodeIdSequence: NodeIdSequence ->
                        NodeSequenceResolutionSucceeded(nodeIdSequence, startLink, endLink)
                    }
            }
            ?: NodeSequenceResolutionFailed("Could not resolve node identifier sequence because of internal error")
    }
}
