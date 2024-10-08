/*
 * This file is generated by jOOQ.
 */
package fi.hsl.jore4.mapmatching.model.tables;


import fi.hsl.jore4.mapmatching.config.jooq.converter.LineStringBinding;
import fi.hsl.jore4.mapmatching.config.jooq.converter.TrafficFlowDirectionTypeConverter;
import fi.hsl.jore4.mapmatching.model.Keys;
import fi.hsl.jore4.mapmatching.model.Routing;
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType;
import fi.hsl.jore4.mapmatching.model.tables.InfrastructureLinkSafelyTraversedByVehicleType.InfrastructureLinkSafelyTraversedByVehicleTypePath;
import fi.hsl.jore4.mapmatching.model.tables.InfrastructureSource.InfrastructureSourcePath;
import fi.hsl.jore4.mapmatching.model.tables.PublicTransportStop.PublicTransportStopPath;
import fi.hsl.jore4.mapmatching.model.tables.TrafficFlowDirection.TrafficFlowDirectionPath;
import fi.hsl.jore4.mapmatching.model.tables.VehicleType.VehicleTypePath;
import fi.hsl.jore4.mapmatching.model.tables.records.InfrastructureLinkRecord;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.geolatte.geom.C2D;
import org.geolatte.geom.LineString;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.InverseForeignKey;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Path;
import org.jooq.PlainSQL;
import org.jooq.QueryPart;
import org.jooq.Record;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Stringly;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * The infrastructure links, e.g. road or rail elements:
 * https://www.transmodel-cen.eu/model/index.htm?goto=2:1:1:1:453
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InfrastructureLink extends TableImpl<InfrastructureLinkRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>routing.infrastructure_link</code>
     */
    public static final InfrastructureLink INFRASTRUCTURE_LINK = new InfrastructureLink();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<InfrastructureLinkRecord> getRecordType() {
        return InfrastructureLinkRecord.class;
    }

    /**
     * The column
     * <code>routing.infrastructure_link.infrastructure_link_id</code>. The
     * local ID of the infrastructure link. The requirement of the ID being of
     * integer type is imposed by pgRouting.
     */
    public final TableField<InfrastructureLinkRecord, Long> INFRASTRUCTURE_LINK_ID = createField(DSL.name("infrastructure_link_id"), SQLDataType.BIGINT.nullable(false), this, "The local ID of the infrastructure link. The requirement of the ID being of integer type is imposed by pgRouting.");

    /**
     * The column
     * <code>routing.infrastructure_link.infrastructure_source_id</code>. The ID
     * of the external source system providing the link data.
     */
    public final TableField<InfrastructureLinkRecord, Integer> INFRASTRUCTURE_SOURCE_ID = createField(DSL.name("infrastructure_source_id"), SQLDataType.INTEGER.nullable(false), this, "The ID of the external source system providing the link data.");

    /**
     * The column <code>routing.infrastructure_link.external_link_id</code>. The
     * ID of the infrastructure link within the external source system providing
     * the link data
     */
    public final TableField<InfrastructureLinkRecord, String> EXTERNAL_LINK_ID = createField(DSL.name("external_link_id"), SQLDataType.CLOB.nullable(false), this, "The ID of the infrastructure link within the external source system providing the link data");

    /**
     * The column
     * <code>routing.infrastructure_link.traffic_flow_direction_type</code>. A
     * numeric enum value for direction of traffic flow allowed on the
     * infrastructure link
     */
    public final TableField<InfrastructureLinkRecord, TrafficFlowDirectionType> TRAFFIC_FLOW_DIRECTION_TYPE = createField(DSL.name("traffic_flow_direction_type"), SQLDataType.INTEGER.nullable(false), this, "A numeric enum value for direction of traffic flow allowed on the infrastructure link", new TrafficFlowDirectionTypeConverter());

    /**
     * The column <code>routing.infrastructure_link.municipality_code</code>.
     * The official code of municipality in which the link is located
     */
    public final TableField<InfrastructureLinkRecord, Integer> MUNICIPALITY_CODE = createField(DSL.name("municipality_code"), SQLDataType.INTEGER, this, "The official code of municipality in which the link is located");

    /**
     * The column <code>routing.infrastructure_link.external_link_type</code>.
     * The link type code defined within the external source system providing
     * the link data
     */
    public final TableField<InfrastructureLinkRecord, Integer> EXTERNAL_LINK_TYPE = createField(DSL.name("external_link_type"), SQLDataType.INTEGER, this, "The link type code defined within the external source system providing the link data");

    /**
     * The column <code>routing.infrastructure_link.external_link_state</code>.
     * The link state code defined within the external source system providing
     * the link data
     */
    public final TableField<InfrastructureLinkRecord, Integer> EXTERNAL_LINK_STATE = createField(DSL.name("external_link_state"), SQLDataType.INTEGER, this, "The link state code defined within the external source system providing the link data");

    /**
     * The column <code>routing.infrastructure_link.name</code>. JSON object
     * containing name of road or street in different localisations
     */
    public final TableField<InfrastructureLinkRecord, JSONB> NAME = createField(DSL.name("name"), SQLDataType.JSONB, this, "JSON object containing name of road or street in different localisations");

    /**
     * The column <code>routing.infrastructure_link.geom</code>. The 2D
     * linestring geometry describing the shape of the infrastructure link. The
     * requirement of two-dimensionality and metric unit is imposed by
     * pgRouting. The EPSG:3067 coordinate system applied is the same as is used
     * in Digiroad.
     */
    public final TableField<InfrastructureLinkRecord, LineString<C2D>> GEOM = createField(DSL.name("geom"), SQLDataType.OTHER.nullable(false), this, "The 2D linestring geometry describing the shape of the infrastructure link. The requirement of two-dimensionality and metric unit is imposed by pgRouting. The EPSG:3067 coordinate system applied is the same as is used in Digiroad.", new LineStringBinding());

    /**
     * The column <code>routing.infrastructure_link.start_node_id</code>. The ID
     * of the start node for the infrastructure link based on its linestring
     * geometry. The node points are resolved and generated by calling
     * `pgr_createTopology` function of pgRouting.
     */
    public final TableField<InfrastructureLinkRecord, Long> START_NODE_ID = createField(DSL.name("start_node_id"), SQLDataType.BIGINT.nullable(false), this, "The ID of the start node for the infrastructure link based on its linestring geometry. The node points are resolved and generated by calling `pgr_createTopology` function of pgRouting.");

    /**
     * The column <code>routing.infrastructure_link.end_node_id</code>. The ID
     * of the end node for the infrastructure link based on its linestring
     * geometry. The node points are resolved and generated by calling
     * `pgr_createTopology` function of pgRouting.
     */
    public final TableField<InfrastructureLinkRecord, Long> END_NODE_ID = createField(DSL.name("end_node_id"), SQLDataType.BIGINT.nullable(false), this, "The ID of the end node for the infrastructure link based on its linestring geometry. The node points are resolved and generated by calling `pgr_createTopology` function of pgRouting.");

    /**
     * The column <code>routing.infrastructure_link.cost</code>. The weight in
     * terms of graph traversal for forward direction of the linestring geometry
     * of the infrastructure link. When negative, the forward direction of the
     * link (edge) will not be part of the graph within the shortest path
     * calculation.
     */
    public final TableField<InfrastructureLinkRecord, Double> COST = createField(DSL.name("cost"), SQLDataType.DOUBLE.nullable(false), this, "The weight in terms of graph traversal for forward direction of the linestring geometry of the infrastructure link. When negative, the forward direction of the link (edge) will not be part of the graph within the shortest path calculation.");

    /**
     * The column <code>routing.infrastructure_link.reverse_cost</code>. The
     * weight in terms of graph traversal for reverse direction of the
     * linestring geometry of the infrastructure link. When negative, the
     * reverse direction of the link (edge) will not be part of the graph within
     * the shortest path calculation.
     */
    public final TableField<InfrastructureLinkRecord, Double> REVERSE_COST = createField(DSL.name("reverse_cost"), SQLDataType.DOUBLE.nullable(false), this, "The weight in terms of graph traversal for reverse direction of the linestring geometry of the infrastructure link. When negative, the reverse direction of the link (edge) will not be part of the graph within the shortest path calculation.");

    private InfrastructureLink(Name alias, Table<InfrastructureLinkRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private InfrastructureLink(Name alias, Table<InfrastructureLinkRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment("The infrastructure links, e.g. road or rail elements: https://www.transmodel-cen.eu/model/index.htm?goto=2:1:1:1:453"), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>routing.infrastructure_link</code> table
     * reference
     */
    public InfrastructureLink(String alias) {
        this(DSL.name(alias), INFRASTRUCTURE_LINK);
    }

    /**
     * Create an aliased <code>routing.infrastructure_link</code> table
     * reference
     */
    public InfrastructureLink(Name alias) {
        this(alias, INFRASTRUCTURE_LINK);
    }

    /**
     * Create a <code>routing.infrastructure_link</code> table reference
     */
    public InfrastructureLink() {
        this(DSL.name("infrastructure_link"), null);
    }

    public <O extends Record> InfrastructureLink(Table<O> path, ForeignKey<O, InfrastructureLinkRecord> childPath, InverseForeignKey<O, InfrastructureLinkRecord> parentPath) {
        super(path, childPath, parentPath, INFRASTRUCTURE_LINK);
    }

    /**
     * A subtype implementing {@link Path} for simplified path-based joins.
     */
    public static class InfrastructureLinkPath extends InfrastructureLink implements Path<InfrastructureLinkRecord> {

        private static final long serialVersionUID = 1L;
        public <O extends Record> InfrastructureLinkPath(Table<O> path, ForeignKey<O, InfrastructureLinkRecord> childPath, InverseForeignKey<O, InfrastructureLinkRecord> parentPath) {
            super(path, childPath, parentPath);
        }
        private InfrastructureLinkPath(Name alias, Table<InfrastructureLinkRecord> aliased) {
            super(alias, aliased);
        }

        @Override
        public InfrastructureLinkPath as(String alias) {
            return new InfrastructureLinkPath(DSL.name(alias), this);
        }

        @Override
        public InfrastructureLinkPath as(Name alias) {
            return new InfrastructureLinkPath(alias, this);
        }

        @Override
        public InfrastructureLinkPath as(Table<?> alias) {
            return new InfrastructureLinkPath(alias.getQualifiedName(), this);
        }
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Routing.ROUTING;
    }

    @Override
    public UniqueKey<InfrastructureLinkRecord> getPrimaryKey() {
        return Keys.INFRASTRUCTURE_LINK_PKEY;
    }

    @Override
    public List<UniqueKey<InfrastructureLinkRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.UK_INFRASTRUCTURE_LINK_EXTERNAL_REF);
    }

    @Override
    public List<ForeignKey<InfrastructureLinkRecord, ?>> getReferences() {
        return Arrays.asList(Keys.INFRASTRUCTURE_LINK__INFRASTRUCTURE_LINK_INFRASTRUCTURE_SOURCE_FKEY, Keys.INFRASTRUCTURE_LINK__INFRASTRUCTURE_LINK_TRAFFIC_FLOW_DIRECTION_FKEY);
    }

    private transient InfrastructureSourcePath _infrastructureSource;

    /**
     * Get the implicit join path to the
     * <code>routing.infrastructure_source</code> table.
     */
    public InfrastructureSourcePath infrastructureSource() {
        if (_infrastructureSource == null)
            _infrastructureSource = new InfrastructureSourcePath(this, Keys.INFRASTRUCTURE_LINK__INFRASTRUCTURE_LINK_INFRASTRUCTURE_SOURCE_FKEY, null);

        return _infrastructureSource;
    }

    private transient TrafficFlowDirectionPath _trafficFlowDirection;

    /**
     * Get the implicit join path to the
     * <code>routing.traffic_flow_direction</code> table.
     */
    public TrafficFlowDirectionPath trafficFlowDirection() {
        if (_trafficFlowDirection == null)
            _trafficFlowDirection = new TrafficFlowDirectionPath(this, Keys.INFRASTRUCTURE_LINK__INFRASTRUCTURE_LINK_TRAFFIC_FLOW_DIRECTION_FKEY, null);

        return _trafficFlowDirection;
    }

    private transient InfrastructureLinkSafelyTraversedByVehicleTypePath _infrastructureLinkSafelyTraversedByVehicleType;

    /**
     * Get the implicit to-many join path to the
     * <code>routing.infrastructure_link_safely_traversed_by_vehicle_type</code>
     * table
     */
    public InfrastructureLinkSafelyTraversedByVehicleTypePath infrastructureLinkSafelyTraversedByVehicleType() {
        if (_infrastructureLinkSafelyTraversedByVehicleType == null)
            _infrastructureLinkSafelyTraversedByVehicleType = new InfrastructureLinkSafelyTraversedByVehicleTypePath(this, null, Keys.INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE__INFRASTRUCTURE_LINK_SAFELY_TRAVERSE_INFRASTRUCTURE_LINK_ID_FKEY.getInverseKey());

        return _infrastructureLinkSafelyTraversedByVehicleType;
    }

    private transient PublicTransportStopPath _publicTransportStop;

    /**
     * Get the implicit to-many join path to the
     * <code>routing.public_transport_stop</code> table
     */
    public PublicTransportStopPath publicTransportStop() {
        if (_publicTransportStop == null)
            _publicTransportStop = new PublicTransportStopPath(this, null, Keys.PUBLIC_TRANSPORT_STOP__PUBLIC_TRANSPORT_STOP_INFRASTRUCTURE_LINK_FKEY.getInverseKey());

        return _publicTransportStop;
    }

    /**
     * Get the implicit many-to-many join path to the
     * <code>routing.vehicle_type</code> table
     */
    public VehicleTypePath vehicleType() {
        return infrastructureLinkSafelyTraversedByVehicleType().vehicleType();
    }

    @Override
    public InfrastructureLink as(String alias) {
        return new InfrastructureLink(DSL.name(alias), this);
    }

    @Override
    public InfrastructureLink as(Name alias) {
        return new InfrastructureLink(alias, this);
    }

    @Override
    public InfrastructureLink as(Table<?> alias) {
        return new InfrastructureLink(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public InfrastructureLink rename(String name) {
        return new InfrastructureLink(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public InfrastructureLink rename(Name name) {
        return new InfrastructureLink(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public InfrastructureLink rename(Table<?> name) {
        return new InfrastructureLink(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public InfrastructureLink where(Condition condition) {
        return new InfrastructureLink(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public InfrastructureLink where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public InfrastructureLink where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public InfrastructureLink where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public InfrastructureLink where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public InfrastructureLink where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public InfrastructureLink where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public InfrastructureLink where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public InfrastructureLink whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public InfrastructureLink whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
