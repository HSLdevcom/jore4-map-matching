package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink

sealed interface NodeSequenceResolutionResult

data class NodeSequenceResolutionSucceeded(
    val nodeIdSequence: NodeIdSequence,
    val pointOnStartLink: SnappedPointOnLink,
    val pointOnEndLink: SnappedPointOnLink
) :
    NodeSequenceResolutionResult

data class NodeSequenceResolutionFailed(val message: String) : NodeSequenceResolutionResult
