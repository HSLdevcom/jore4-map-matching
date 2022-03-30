package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.INodeRepository
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Component
class NodeServiceInternalImpl @Autowired constructor(val nodeRepository: INodeRepository) : INodeServiceInternal {

    @Transactional(readOnly = true, noRollbackFor = [IllegalStateException::class])
    override fun resolveNodeIdSequence(nodeSequenceCandidates: NodeSequenceCandidates,
                                       vehicleType: VehicleType,
                                       bufferAreaRestriction: BufferAreaRestriction?)
        : NodeIdSequence {

        val nodeIdSequences: List<NodeIdSequence> = nodeSequenceCandidates.nodeIdSequences

        if (nodeIdSequences.size == 1) {
            return nodeIdSequences.first()
        }

        LOGGER.debug {
            "Resolving best node identifier sequence among candidates: ${joinToLogString(nodeIdSequences)}"
        }

        return nodeRepository.resolveNodeSequence(nodeIdSequences,
                                                  vehicleType,
                                                  bufferAreaRestriction)
            ?: throw IllegalStateException(
                "Could not resolve node identifier sequence from ${nodeSequenceCandidates.prettyPrint()}"
            )
    }
}
