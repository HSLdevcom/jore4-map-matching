package fi.hsl.jore4.mapmatching.service.node

interface INodeServiceInternal {

    /**
     * Resolves an optimal sequence of identifiers for infrastructure network
     * nodes based on the [NodeResolutionParams] parameter.
     */
    fun resolveNodeSequence(params: NodeResolutionParams): List<Long>
}
