/*
 * This file is generated by jOOQ.
 */
package fi.hsl.jore4.mapmatching.model.tables.records;


import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType;
import fi.hsl.jore4.mapmatching.model.tables.InfrastructureLink;

import org.geolatte.geom.C2D;
import org.geolatte.geom.LineString;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * The infrastructure links, e.g. road or rail elements:
 * https://www.transmodel-cen.eu/model/index.htm?goto=2:1:1:1:453
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InfrastructureLinkRecord extends UpdatableRecordImpl<InfrastructureLinkRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for
     * <code>routing.infrastructure_link.infrastructure_link_id</code>. The
     * local ID of the infrastructure link. The requirement of the ID being of
     * integer type is imposed by pgRouting.
     */
    public void setInfrastructureLinkId(Long value) {
        set(0, value);
    }

    /**
     * Getter for
     * <code>routing.infrastructure_link.infrastructure_link_id</code>. The
     * local ID of the infrastructure link. The requirement of the ID being of
     * integer type is imposed by pgRouting.
     */
    public Long getInfrastructureLinkId() {
        return (Long) get(0);
    }

    /**
     * Setter for
     * <code>routing.infrastructure_link.infrastructure_source_id</code>. The ID
     * of the external source system providing the link data.
     */
    public void setInfrastructureSourceId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for
     * <code>routing.infrastructure_link.infrastructure_source_id</code>. The ID
     * of the external source system providing the link data.
     */
    public Integer getInfrastructureSourceId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>routing.infrastructure_link.external_link_id</code>. The
     * ID of the infrastructure link within the external source system providing
     * the link data
     */
    public void setExternalLinkId(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.external_link_id</code>. The
     * ID of the infrastructure link within the external source system providing
     * the link data
     */
    public String getExternalLinkId() {
        return (String) get(2);
    }

    /**
     * Setter for
     * <code>routing.infrastructure_link.traffic_flow_direction_type</code>. A
     * numeric enum value for direction of traffic flow allowed on the
     * infrastructure link
     */
    public void setTrafficFlowDirectionType(TrafficFlowDirectionType value) {
        set(3, value);
    }

    /**
     * Getter for
     * <code>routing.infrastructure_link.traffic_flow_direction_type</code>. A
     * numeric enum value for direction of traffic flow allowed on the
     * infrastructure link
     */
    public TrafficFlowDirectionType getTrafficFlowDirectionType() {
        return (TrafficFlowDirectionType) get(3);
    }

    /**
     * Setter for <code>routing.infrastructure_link.municipality_code</code>.
     * The official code of municipality in which the link is located
     */
    public void setMunicipalityCode(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.municipality_code</code>.
     * The official code of municipality in which the link is located
     */
    public Integer getMunicipalityCode() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>routing.infrastructure_link.external_link_type</code>.
     * The link type code defined within the external source system providing
     * the link data
     */
    public void setExternalLinkType(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.external_link_type</code>.
     * The link type code defined within the external source system providing
     * the link data
     */
    public Integer getExternalLinkType() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>routing.infrastructure_link.external_link_state</code>.
     * The link state code defined within the external source system providing
     * the link data
     */
    public void setExternalLinkState(Integer value) {
        set(6, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.external_link_state</code>.
     * The link state code defined within the external source system providing
     * the link data
     */
    public Integer getExternalLinkState() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>routing.infrastructure_link.name</code>. JSON object
     * containing name of road or street in different localisations
     */
    public void setName(JSONB value) {
        set(7, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.name</code>. JSON object
     * containing name of road or street in different localisations
     */
    public JSONB getName() {
        return (JSONB) get(7);
    }

    /**
     * Setter for <code>routing.infrastructure_link.geom</code>. The 2D
     * linestring geometry describing the shape of the infrastructure link. The
     * requirement of two-dimensionality and metric unit is imposed by
     * pgRouting. The EPSG:3067 coordinate system applied is the same as is used
     * in Digiroad.
     */
    public void setGeom(LineString<C2D> value) {
        set(8, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.geom</code>. The 2D
     * linestring geometry describing the shape of the infrastructure link. The
     * requirement of two-dimensionality and metric unit is imposed by
     * pgRouting. The EPSG:3067 coordinate system applied is the same as is used
     * in Digiroad.
     */
    public LineString<C2D> getGeom() {
        return (LineString<C2D>) get(8);
    }

    /**
     * Setter for <code>routing.infrastructure_link.start_node_id</code>. The ID
     * of the start node for the infrastructure link based on its linestring
     * geometry. The node points are resolved and generated by calling
     * `pgr_createTopology` function of pgRouting.
     */
    public void setStartNodeId(Long value) {
        set(9, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.start_node_id</code>. The ID
     * of the start node for the infrastructure link based on its linestring
     * geometry. The node points are resolved and generated by calling
     * `pgr_createTopology` function of pgRouting.
     */
    public Long getStartNodeId() {
        return (Long) get(9);
    }

    /**
     * Setter for <code>routing.infrastructure_link.end_node_id</code>. The ID
     * of the end node for the infrastructure link based on its linestring
     * geometry. The node points are resolved and generated by calling
     * `pgr_createTopology` function of pgRouting.
     */
    public void setEndNodeId(Long value) {
        set(10, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.end_node_id</code>. The ID
     * of the end node for the infrastructure link based on its linestring
     * geometry. The node points are resolved and generated by calling
     * `pgr_createTopology` function of pgRouting.
     */
    public Long getEndNodeId() {
        return (Long) get(10);
    }

    /**
     * Setter for <code>routing.infrastructure_link.cost</code>. The weight in
     * terms of graph traversal for forward direction of the linestring geometry
     * of the infrastructure link. When negative, the forward direction of the
     * link (edge) will not be part of the graph within the shortest path
     * calculation.
     */
    public void setCost(Double value) {
        set(11, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.cost</code>. The weight in
     * terms of graph traversal for forward direction of the linestring geometry
     * of the infrastructure link. When negative, the forward direction of the
     * link (edge) will not be part of the graph within the shortest path
     * calculation.
     */
    public Double getCost() {
        return (Double) get(11);
    }

    /**
     * Setter for <code>routing.infrastructure_link.reverse_cost</code>. The
     * weight in terms of graph traversal for reverse direction of the
     * linestring geometry of the infrastructure link. When negative, the
     * reverse direction of the link (edge) will not be part of the graph within
     * the shortest path calculation.
     */
    public void setReverseCost(Double value) {
        set(12, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.reverse_cost</code>. The
     * weight in terms of graph traversal for reverse direction of the
     * linestring geometry of the infrastructure link. When negative, the
     * reverse direction of the link (edge) will not be part of the graph within
     * the shortest path calculation.
     */
    public Double getReverseCost() {
        return (Double) get(12);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached InfrastructureLinkRecord
     */
    public InfrastructureLinkRecord() {
        super(InfrastructureLink.INFRASTRUCTURE_LINK);
    }

    /**
     * Create a detached, initialised InfrastructureLinkRecord
     */
    public InfrastructureLinkRecord(Long infrastructureLinkId, Integer infrastructureSourceId, String externalLinkId, TrafficFlowDirectionType trafficFlowDirectionType, Integer municipalityCode, Integer externalLinkType, Integer externalLinkState, JSONB name, LineString<C2D> geom, Long startNodeId, Long endNodeId, Double cost, Double reverseCost) {
        super(InfrastructureLink.INFRASTRUCTURE_LINK);

        setInfrastructureLinkId(infrastructureLinkId);
        setInfrastructureSourceId(infrastructureSourceId);
        setExternalLinkId(externalLinkId);
        setTrafficFlowDirectionType(trafficFlowDirectionType);
        setMunicipalityCode(municipalityCode);
        setExternalLinkType(externalLinkType);
        setExternalLinkState(externalLinkState);
        setName(name);
        setGeom(geom);
        setStartNodeId(startNodeId);
        setEndNodeId(endNodeId);
        setCost(cost);
        setReverseCost(reverseCost);
        resetChangedOnNotNull();
    }
}
