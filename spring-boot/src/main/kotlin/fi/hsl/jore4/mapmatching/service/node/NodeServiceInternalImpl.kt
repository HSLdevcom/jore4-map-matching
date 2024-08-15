package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.INodeRepository
import fi.hsl.jore4.mapmatching.repository.routing.NodeSequenceCandidate
import fi.hsl.jore4.mapmatching.util.InternalService
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@InternalService
class NodeServiceInternalImpl
    @Autowired
    constructor(
        val nodeRepository: INodeRepository,
    ) : INodeServiceInternal {
        @Transactional(readOnly = true, noRollbackFor = [IllegalStateException::class])
        override fun resolveNodeIdSequence(
            nodeSequenceCandidates: List<NodeSequenceCandidatesBetweenSnappedLinks>,
            vehicleType: VehicleType,
            bufferAreaRestriction: BufferAreaRestriction?,
        ): NodeSequenceResolutionResult {
            fun getDebugLogMessageForNodeSequenceCombinations(nodeIdSequences: List<NodeIdSequence>) =
                "Resolving best node identifier sequence from combinations: ${joinToLogString(nodeIdSequences)}"

            if (nodeSequenceCandidates.size == 1) {
                val candidatesBetweenLinks: NodeSequenceCandidatesBetweenSnappedLinks = nodeSequenceCandidates.first()
                val nodeIdSequences: List<NodeIdSequence> = candidatesBetweenLinks.nodeIdSequences

                if (nodeIdSequences.size == 1) {
                    return NodeSequenceResolutionSucceeded(
                        nodeIdSequences.first(),
                        candidatesBetweenLinks.pointOnStartLink,
                        candidatesBetweenLinks.pointOnEndLink,
                    )
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

            fun flattenCandidates(candidates: NodeSequenceCandidatesBetweenSnappedLinks): List<NodeSequenceCandidate> =
                candidates.nodeIdSequences.map { nodeIdSequence ->
                    NodeSequenceCandidate(
                        candidates.pointOnStartLink.infrastructureLinkId,
                        candidates.pointOnEndLink.infrastructureLinkId,
                        nodeIdSequence,
                    )
                }

            val possibleNodeSequences: Map<Pair<InfrastructureLinkId, InfrastructureLinkId>, NodeIdSequence>? =
                nodeSequenceCandidates.firstNotNullOfOrNull { candidatesBetweenTwoLinks ->

                    val bufferAreaRestrictionWithTerminusLinkIds: BufferAreaRestriction? =
                        bufferAreaRestriction?.run {
                            BufferAreaRestriction.from(
                                lineGeometry,
                                bufferRadiusInMeters,
                                candidatesBetweenTwoLinks.pointOnStartLink,
                                candidatesBetweenTwoLinks.pointOnEndLink,
                            )
                        }

                    nodeRepository
                        .resolveBestNodeSequences(
                            flattenCandidates(candidatesBetweenTwoLinks),
                            vehicleType,
                            bufferAreaRestrictionWithTerminusLinkIds,
                        ).takeIf { it.isNotEmpty() }
                }

            if (possibleNodeSequences == null) {
                val flattenedNodeSequenceCandidates: List<NodeSequenceCandidate> =
                    nodeSequenceCandidates.flatMap { flattenCandidates(it) }

                return NodeSequenceResolutionFailed(
                    "Could not resolve node identifier sequence from ${joinToLogString(
                        flattenedNodeSequenceCandidates,
                    )}",
                )
            }

            return nodeSequenceCandidates
                .firstNotNullOfOrNull { candidatesBetweenTwoLinks ->
                    val pointOnStartLink: SnappedPointOnLink = candidatesBetweenTwoLinks.pointOnStartLink
                    val pointOnEndLink: SnappedPointOnLink = candidatesBetweenTwoLinks.pointOnEndLink

                    val nodeSeqKey: Pair<InfrastructureLinkId, InfrastructureLinkId> =
                        pointOnStartLink.infrastructureLinkId to pointOnEndLink.infrastructureLinkId

                    possibleNodeSequences[nodeSeqKey]
                        ?.let { nodeIdSequence: NodeIdSequence ->
                            NodeSequenceResolutionSucceeded(nodeIdSequence, pointOnStartLink, pointOnEndLink)
                        }
                }
                ?: NodeSequenceResolutionFailed("Could not resolve node identifier sequence because of internal error")
        }
    }
