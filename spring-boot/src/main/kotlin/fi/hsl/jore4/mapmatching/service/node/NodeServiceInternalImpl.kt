package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.INodeRepository
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NodeServiceInternalImpl @Autowired constructor(val nodeRepository: INodeRepository) : INodeServiceInternal {

    @Transactional(readOnly = true, noRollbackFor = [IllegalStateException::class])
    override fun resolveNodeIdSequence(nodeSequenceAlternatives: NodeSequenceAlternatives,
                                       vehicleType: VehicleType)
        : List<Long> {

        val nodeIdSequences: List<List<Long>> = nodeSequenceAlternatives.nodeIdSequences

        if (nodeIdSequences.size == 1) {
            return nodeIdSequences.first()
        }

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Resolving best node identifier sequence among alternatives: {}",
                         joinToLogString(nodeIdSequences))
        }

        return nodeRepository.resolveNodeSequence(nodeSequenceAlternatives.startLinkId,
                                                  nodeSequenceAlternatives.endLinkId,
                                                  nodeIdSequences,
                                                  vehicleType)
            ?: throw IllegalStateException(
                "Could not resolve node identifier sequence from ${nodeSequenceAlternatives.toCompactString()}")
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(NodeServiceInternalImpl::class.java)
    }
}
