package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

sealed interface NodeSequenceResolutionResult

data class NodeSequenceResolutionSucceeded(val nodeIdSequence: NodeIdSequence,
                                           val startLink: SnappedLinkState,
                                           val endLink: SnappedLinkState)
    : NodeSequenceResolutionResult

data class NodeSequenceResolutionFailed(val message: String) : NodeSequenceResolutionResult
