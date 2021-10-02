package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.repository.routing.NodeRepository
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class NodeServiceInternal @Autowired constructor(val nodeRepository: NodeRepository) {

    @Transactional(readOnly = true)
    fun resolveSimpleNodeSequence(params: NodeResolutionParams): List<Int> {
        val nodeSequences: Set<List<Int>> = params.resolvePossibleNodeSequences()

        if (nodeSequences.size == 1) {
            return nodeSequences.first()
        }

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Resolving best node sequence from alternatives: {}", joinToLogString(nodeSequences))
        }

        val startLinkId: String = params.firstLink.linkId
        val endLinkId: String = params.lastLink.linkId

        return nodeRepository.resolveSimpleNodeSequence(startLinkId, endLinkId, nodeSequences)
            ?: throw IllegalStateException("Could not resolve node sequence from ${params.toCompactString()}")
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(NodeServiceInternal::class.java)
    }
}
