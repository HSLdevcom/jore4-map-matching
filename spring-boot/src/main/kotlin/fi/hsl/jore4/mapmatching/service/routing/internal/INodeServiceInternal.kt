package fi.hsl.jore4.mapmatching.service.routing.internal

interface INodeServiceInternal {

    /**
     * Resolves an optimal sequence of infrastructure node IDs based on the
     * given parameter object. The {@link NodeResolutionParams} guarantees
     * that the input node sequence does not include subsequent duplicate
     * values.
     *
     * It is required that:
     * (1) IDs of both endpoint nodes for the first and last link (determined by
     * parameter) are included in the list. This may sound trivial but requires
     * attention to detail within implementation.
     */
    fun resolveNodeSequence(params: NodeResolutionParams): List<Long>
}
