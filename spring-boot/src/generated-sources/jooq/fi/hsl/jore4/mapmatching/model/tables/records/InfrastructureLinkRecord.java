/*
 * This file is generated by jOOQ.
 */
package fi.hsl.jore4.mapmatching.model.tables.records;


import fi.hsl.jore4.mapmatching.model.tables.InfrastructureLink;

import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.Record13;
import org.jooq.Row13;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * The infrastructure links, e.g. road or rail elements: https://www.transmodel-cen.eu/model/index.htm?goto=2:1:1:1:453
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InfrastructureLinkRecord extends UpdatableRecordImpl<InfrastructureLinkRecord> implements Record13<Long, Integer, String, Integer, Integer, Integer, Integer, JSONB, Object, Long, Long, Double, Double> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>routing.infrastructure_link.infrastructure_link_id</code>. The local ID of the infrastructure link. The requirement of the ID being of integer type is imposed by pgRouting.
     */
    public void setInfrastructureLinkId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.infrastructure_link_id</code>. The local ID of the infrastructure link. The requirement of the ID being of integer type is imposed by pgRouting.
     */
    public Long getInfrastructureLinkId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>routing.infrastructure_link.infrastructure_source_id</code>. The ID of the external source system providing the link data.
     */
    public void setInfrastructureSourceId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.infrastructure_source_id</code>. The ID of the external source system providing the link data.
     */
    public Integer getInfrastructureSourceId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>routing.infrastructure_link.external_link_id</code>. The ID of the infrastructure link within the external source system providing the link data
     */
    public void setExternalLinkId(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.external_link_id</code>. The ID of the infrastructure link within the external source system providing the link data
     */
    public String getExternalLinkId() {
        return (String) get(2);
    }

    /**
     * Setter for <code>routing.infrastructure_link.traffic_flow_direction_type</code>. A numeric enum value for direction of traffic flow allowed on the infrastructure link
     */
    public void setTrafficFlowDirectionType(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.traffic_flow_direction_type</code>. A numeric enum value for direction of traffic flow allowed on the infrastructure link
     */
    public Integer getTrafficFlowDirectionType() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>routing.infrastructure_link.municipality_code</code>. The official code of municipality in which the link is located
     */
    public void setMunicipalityCode(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.municipality_code</code>. The official code of municipality in which the link is located
     */
    public Integer getMunicipalityCode() {
        return (Integer) get(4);
    }

    /**
     * Setter for <code>routing.infrastructure_link.external_link_type</code>. The link type code defined within the external source system providing the link data
     */
    public void setExternalLinkType(Integer value) {
        set(5, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.external_link_type</code>. The link type code defined within the external source system providing the link data
     */
    public Integer getExternalLinkType() {
        return (Integer) get(5);
    }

    /**
     * Setter for <code>routing.infrastructure_link.external_link_state</code>. The link state code defined within the external source system providing the link data
     */
    public void setExternalLinkState(Integer value) {
        set(6, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.external_link_state</code>. The link state code defined within the external source system providing the link data
     */
    public Integer getExternalLinkState() {
        return (Integer) get(6);
    }

    /**
     * Setter for <code>routing.infrastructure_link.name</code>. JSON object containing name of road or street in different localisations
     */
    public void setName(JSONB value) {
        set(7, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.name</code>. JSON object containing name of road or street in different localisations
     */
    public JSONB getName() {
        return (JSONB) get(7);
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    public void setGeom(Object value) {
        set(8, value);
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    public Object getGeom() {
        return get(8);
    }

    /**
     * Setter for <code>routing.infrastructure_link.start_node_id</code>. The ID of the start node for the infrastructure link based on its linestring geometry. The node points are resolved and generated by calling `pgr_createTopology` function of pgRouting.
     */
    public void setStartNodeId(Long value) {
        set(9, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.start_node_id</code>. The ID of the start node for the infrastructure link based on its linestring geometry. The node points are resolved and generated by calling `pgr_createTopology` function of pgRouting.
     */
    public Long getStartNodeId() {
        return (Long) get(9);
    }

    /**
     * Setter for <code>routing.infrastructure_link.end_node_id</code>. The ID of the end node for the infrastructure link based on its linestring geometry. The node points are resolved and generated by calling `pgr_createTopology` function of pgRouting.
     */
    public void setEndNodeId(Long value) {
        set(10, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.end_node_id</code>. The ID of the end node for the infrastructure link based on its linestring geometry. The node points are resolved and generated by calling `pgr_createTopology` function of pgRouting.
     */
    public Long getEndNodeId() {
        return (Long) get(10);
    }

    /**
     * Setter for <code>routing.infrastructure_link.cost</code>. The weight in terms of graph traversal for forward direction of the linestring geometry of the infrastructure link. When negative, the forward direction of the link (edge) will not be part of the graph within the shortest path calculation.
     */
    public void setCost(Double value) {
        set(11, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.cost</code>. The weight in terms of graph traversal for forward direction of the linestring geometry of the infrastructure link. When negative, the forward direction of the link (edge) will not be part of the graph within the shortest path calculation.
     */
    public Double getCost() {
        return (Double) get(11);
    }

    /**
     * Setter for <code>routing.infrastructure_link.reverse_cost</code>. The weight in terms of graph traversal for reverse direction of the linestring geometry of the infrastructure link. When negative, the reverse direction of the link (edge) will not be part of the graph within the shortest path calculation.
     */
    public void setReverseCost(Double value) {
        set(12, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link.reverse_cost</code>. The weight in terms of graph traversal for reverse direction of the linestring geometry of the infrastructure link. When negative, the reverse direction of the link (edge) will not be part of the graph within the shortest path calculation.
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
    // Record13 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row13<Long, Integer, String, Integer, Integer, Integer, Integer, JSONB, Object, Long, Long, Double, Double> fieldsRow() {
        return (Row13) super.fieldsRow();
    }

    @Override
    public Row13<Long, Integer, String, Integer, Integer, Integer, Integer, JSONB, Object, Long, Long, Double, Double> valuesRow() {
        return (Row13) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.INFRASTRUCTURE_LINK_ID;
    }

    @Override
    public Field<Integer> field2() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.INFRASTRUCTURE_SOURCE_ID;
    }

    @Override
    public Field<String> field3() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.EXTERNAL_LINK_ID;
    }

    @Override
    public Field<Integer> field4() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.TRAFFIC_FLOW_DIRECTION_TYPE;
    }

    @Override
    public Field<Integer> field5() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.MUNICIPALITY_CODE;
    }

    @Override
    public Field<Integer> field6() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.EXTERNAL_LINK_TYPE;
    }

    @Override
    public Field<Integer> field7() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.EXTERNAL_LINK_STATE;
    }

    @Override
    public Field<JSONB> field8() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.NAME;
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    @Override
    public Field<Object> field9() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.GEOM;
    }

    @Override
    public Field<Long> field10() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.START_NODE_ID;
    }

    @Override
    public Field<Long> field11() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.END_NODE_ID;
    }

    @Override
    public Field<Double> field12() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.COST;
    }

    @Override
    public Field<Double> field13() {
        return InfrastructureLink.INFRASTRUCTURE_LINK.REVERSE_COST;
    }

    @Override
    public Long component1() {
        return getInfrastructureLinkId();
    }

    @Override
    public Integer component2() {
        return getInfrastructureSourceId();
    }

    @Override
    public String component3() {
        return getExternalLinkId();
    }

    @Override
    public Integer component4() {
        return getTrafficFlowDirectionType();
    }

    @Override
    public Integer component5() {
        return getMunicipalityCode();
    }

    @Override
    public Integer component6() {
        return getExternalLinkType();
    }

    @Override
    public Integer component7() {
        return getExternalLinkState();
    }

    @Override
    public JSONB component8() {
        return getName();
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    @Override
    public Object component9() {
        return getGeom();
    }

    @Override
    public Long component10() {
        return getStartNodeId();
    }

    @Override
    public Long component11() {
        return getEndNodeId();
    }

    @Override
    public Double component12() {
        return getCost();
    }

    @Override
    public Double component13() {
        return getReverseCost();
    }

    @Override
    public Long value1() {
        return getInfrastructureLinkId();
    }

    @Override
    public Integer value2() {
        return getInfrastructureSourceId();
    }

    @Override
    public String value3() {
        return getExternalLinkId();
    }

    @Override
    public Integer value4() {
        return getTrafficFlowDirectionType();
    }

    @Override
    public Integer value5() {
        return getMunicipalityCode();
    }

    @Override
    public Integer value6() {
        return getExternalLinkType();
    }

    @Override
    public Integer value7() {
        return getExternalLinkState();
    }

    @Override
    public JSONB value8() {
        return getName();
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    @Override
    public Object value9() {
        return getGeom();
    }

    @Override
    public Long value10() {
        return getStartNodeId();
    }

    @Override
    public Long value11() {
        return getEndNodeId();
    }

    @Override
    public Double value12() {
        return getCost();
    }

    @Override
    public Double value13() {
        return getReverseCost();
    }

    @Override
    public InfrastructureLinkRecord value1(Long value) {
        setInfrastructureLinkId(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value2(Integer value) {
        setInfrastructureSourceId(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value3(String value) {
        setExternalLinkId(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value4(Integer value) {
        setTrafficFlowDirectionType(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value5(Integer value) {
        setMunicipalityCode(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value6(Integer value) {
        setExternalLinkType(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value7(Integer value) {
        setExternalLinkState(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value8(JSONB value) {
        setName(value);
        return this;
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    @Override
    public InfrastructureLinkRecord value9(Object value) {
        setGeom(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value10(Long value) {
        setStartNodeId(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value11(Long value) {
        setEndNodeId(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value12(Double value) {
        setCost(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord value13(Double value) {
        setReverseCost(value);
        return this;
    }

    @Override
    public InfrastructureLinkRecord values(Long value1, Integer value2, String value3, Integer value4, Integer value5, Integer value6, Integer value7, JSONB value8, Object value9, Long value10, Long value11, Double value12, Double value13) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        value13(value13);
        return this;
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
    public InfrastructureLinkRecord(Long infrastructureLinkId, Integer infrastructureSourceId, String externalLinkId, Integer trafficFlowDirectionType, Integer municipalityCode, Integer externalLinkType, Integer externalLinkState, JSONB name, Object geom, Long startNodeId, Long endNodeId, Double cost, Double reverseCost) {
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
    }
}