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

    @Transactional(readOnly = true)
    override fun resolveNodeSequence(nodeSequenceProducer: NodeSequenceProducer,
                                     vehicleType: VehicleType)
        : List<Long> {

        val nodeSequences: Set<List<Long>> = nodeSequenceProducer.resolvePossibleNodeSequences()

        if (nodeSequences.size == 1) {
            return nodeSequences.first()
        }

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Resolving best node sequence among alternatives: {}", joinToLogString(nodeSequences))
        }

        val startLinkId: Long = nodeSequenceProducer.firstLink.infrastructureLinkId
        val endLinkId: Long = nodeSequenceProducer.lastLink.infrastructureLinkId

        return nodeRepository.resolveNodeSequence(startLinkId,
                                                  endLinkId,
                                                  nodeSequences,
                                                  vehicleType)
            ?: throw IllegalStateException(
                "Could not resolve node sequence from ${nodeSequenceProducer.toCompactString()}")
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(NodeServiceInternalImpl::class.java)
    }
}
