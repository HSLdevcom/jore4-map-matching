package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates

data class NetworkNodeParams(val firstLinkEndpointIds: Pair<Long, Long>,
                             val lastLinkEndPointIds: Pair<Long, Long>,
                             val interimNodeIds: List<Long>) {

    internal fun getNodeSequenceVariants(): NetworkNodeSequences {
        val listOfSequences: List<List<Long>> = firstLinkEndpointIds.toList()
            .flatMap { startNodeId ->
                lastLinkEndPointIds
                    .toList()
                    .map { endNodeId ->

                        val networkNodes = mutableListOf(startNodeId)
                        networkNodes.addAll(interimNodeIds)
                        networkNodes.add(endNodeId)

                        filterOutConsecutiveDuplicates(networkNodes)
                    }
            }

        // Do a sanity check.
        if (listOfSequences.size != 4) {
            throw IllegalStateException("Should have 4 node sequences")
        }

        return NetworkNodeSequences(listOfSequences[0], listOfSequences[1], listOfSequences[2], listOfSequences[3])
    }
}
