package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.node
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator
import org.quicktheories.core.Gen
import org.quicktheories.generators.SourceDSL.integers

object NodeResolutionParamsGenerator {

    private val VIA_NODES: Gen<List<NodeProximity>> = integers().between(1, 4).flatMap { numberOfViaNodes ->
        when (numberOfViaNodes) {
            1 -> node().map { node -> listOf(node) }
            2 -> node().zip(node()) { node1, node2 -> listOf(node1, node2) }
            3 -> node().zip(node(), node()) { node1, node2, node3 -> listOf(node1, node2, node3) }
            4 -> node().zip(node(), node(), node()) { node1, node2, node3, node4 -> listOf(node1, node2, node3, node4) }
            else -> throw IllegalStateException("should not end up here")
        }
    }

    // Single link

    internal val SINGLE_LINK_WITH_VIA_NODES: Gen<NodeResolutionParams> = SnappedLinkStateGenerator
        .snappedLinkState()
        .zip(VIA_NODES) { link, viaNodes -> NodeResolutionParams(link, viaNodes, link) }

    internal val SINGLE_LINK_WITHOUT_VIA_NODES: Gen<NodeResolutionParams> = SnappedLinkStateGenerator
        .snappedLinkState()
        .map { link -> NodeResolutionParams(link, emptyList(), link) }

    // Two unconnected links (no common node)

    internal val TWO_UNCONNECTED_LINKS_WITH_VIA_NODES: Gen<NodeResolutionParams> = SnappedLinkStateGenerator
        .twoUnconnectedLinks()
        .zip(VIA_NODES) { (startLink, endLink), viaNodes -> NodeResolutionParams(startLink, viaNodes, endLink) }

    internal val TWO_UNCONNECTED_LINKS_WITHOUT_VIA_NODES: Gen<NodeResolutionParams> = SnappedLinkStateGenerator
        .twoUnconnectedLinks()
        .map { (startLink, endLink) -> NodeResolutionParams(startLink, emptyList(), endLink) }

    // Two connected links (with shared common node)

    internal val TWO_CONNECTED_LINKS_WITH_VIA_NODES: Gen<NodeResolutionParams> = SnappedLinkStateGenerator
        .twoConnectedLinks()
        .zip(VIA_NODES) { (startLink, endLink), viaNodes -> NodeResolutionParams(startLink, viaNodes, endLink) }

    internal val TWO_CONNECTED_LINKS_WITHOUT_VIA_NODES: Gen<NodeResolutionParams> = SnappedLinkStateGenerator
        .twoConnectedLinks()
        .map { (startLink, endLink) -> NodeResolutionParams(startLink, emptyList(), endLink) }
}
