/*
 * This file is generated by jOOQ.
 */
package fi.hsl.jore4.mapmatching.model.tables;


import fi.hsl.jore4.mapmatching.config.jooq.converter.PointBinding;
import fi.hsl.jore4.mapmatching.model.Keys;
import fi.hsl.jore4.mapmatching.model.Routing;
import fi.hsl.jore4.mapmatching.model.tables.InfrastructureLink.InfrastructureLinkPath;
import fi.hsl.jore4.mapmatching.model.tables.InfrastructureSource.InfrastructureSourcePath;
import fi.hsl.jore4.mapmatching.model.tables.records.PublicTransportStopRecord;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.geolatte.geom.C2D;
import org.geolatte.geom.Point;
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
 * The public transport stops imported from Digiroad export
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PublicTransportStop extends TableImpl<PublicTransportStopRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>routing.public_transport_stop</code>
     */
    public static final PublicTransportStop PUBLIC_TRANSPORT_STOP = new PublicTransportStop();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<PublicTransportStopRecord> getRecordType() {
        return PublicTransportStopRecord.class;
    }

    /**
     * The column
     * <code>routing.public_transport_stop.public_transport_stop_id</code>. The
     * local ID of the public transport stop
     */
    public final TableField<PublicTransportStopRecord, Long> PUBLIC_TRANSPORT_STOP_ID = createField(DSL.name("public_transport_stop_id"), SQLDataType.BIGINT.nullable(false), this, "The local ID of the public transport stop");

    /**
     * The column
     * <code>routing.public_transport_stop.public_transport_stop_national_id</code>.
     * The national (persistent) ID for the public transport stop
     */
    public final TableField<PublicTransportStopRecord, Integer> PUBLIC_TRANSPORT_STOP_NATIONAL_ID = createField(DSL.name("public_transport_stop_national_id"), SQLDataType.INTEGER, this, "The national (persistent) ID for the public transport stop");

    /**
     * The column
     * <code>routing.public_transport_stop.located_on_infrastructure_link_id</code>.
     * The ID of the infrastructure link on which the stop is located
     */
    public final TableField<PublicTransportStopRecord, Long> LOCATED_ON_INFRASTRUCTURE_LINK_ID = createField(DSL.name("located_on_infrastructure_link_id"), SQLDataType.BIGINT.nullable(false), this, "The ID of the infrastructure link on which the stop is located");

    /**
     * The column
     * <code>routing.public_transport_stop.infrastructure_source_id</code>. The
     * ID of the external source system providing the stop data
     */
    public final TableField<PublicTransportStopRecord, Integer> INFRASTRUCTURE_SOURCE_ID = createField(DSL.name("infrastructure_source_id"), SQLDataType.INTEGER.nullable(false), this, "The ID of the external source system providing the stop data");

    /**
     * The column
     * <code>routing.public_transport_stop.is_on_direction_of_link_forward_traversal</code>.
     * Is the direction of traffic on this stop the same as the direction of the
     * linestring describing the infrastructure link? If TRUE, the stop lies in
     * the direction of the linestring. If FALSE, the stop lies in the reverse
     * direction of the linestring. If NULL, the direction is undefined.
     */
    public final TableField<PublicTransportStopRecord, Boolean> IS_ON_DIRECTION_OF_LINK_FORWARD_TRAVERSAL = createField(DSL.name("is_on_direction_of_link_forward_traversal"), SQLDataType.BOOLEAN, this, "Is the direction of traffic on this stop the same as the direction of the linestring describing the infrastructure link? If TRUE, the stop lies in the direction of the linestring. If FALSE, the stop lies in the reverse direction of the linestring. If NULL, the direction is undefined.");

    /**
     * The column
     * <code>routing.public_transport_stop.distance_from_link_start_in_meters</code>.
     * The measure or M value of the stop from the start of the linestring
     * (linear geometry) describing the infrastructure link. The SI unit is the
     * meter.
     */
    public final TableField<PublicTransportStopRecord, Double> DISTANCE_FROM_LINK_START_IN_METERS = createField(DSL.name("distance_from_link_start_in_meters"), SQLDataType.DOUBLE.nullable(false), this, "The measure or M value of the stop from the start of the linestring (linear geometry) describing the infrastructure link. The SI unit is the meter.");

    /**
     * The column <code>routing.public_transport_stop.municipality_code</code>.
     * The official code of municipality in which the stop is located
     */
    public final TableField<PublicTransportStopRecord, Integer> MUNICIPALITY_CODE = createField(DSL.name("municipality_code"), SQLDataType.INTEGER, this, "The official code of municipality in which the stop is located");

    /**
     * The column <code>routing.public_transport_stop.name</code>. JSON object
     * containing name in different localisations
     */
    public final TableField<PublicTransportStopRecord, JSONB> NAME = createField(DSL.name("name"), SQLDataType.JSONB, this, "JSON object containing name in different localisations");

    /**
     * The column <code>routing.public_transport_stop.geom</code>. The 2D point
     * geometry describing the location of the public transport stop. The
     * EPSG:3067 coordinate system applied is the same as is used in Digiroad.
     */
    public final TableField<PublicTransportStopRecord, Point<C2D>> GEOM = createField(DSL.name("geom"), SQLDataType.OTHER.nullable(false), this, "The 2D point geometry describing the location of the public transport stop. The EPSG:3067 coordinate system applied is the same as is used in Digiroad.", new PointBinding());

    private PublicTransportStop(Name alias, Table<PublicTransportStopRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private PublicTransportStop(Name alias, Table<PublicTransportStopRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment("The public transport stops imported from Digiroad export"), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>routing.public_transport_stop</code> table
     * reference
     */
    public PublicTransportStop(String alias) {
        this(DSL.name(alias), PUBLIC_TRANSPORT_STOP);
    }

    /**
     * Create an aliased <code>routing.public_transport_stop</code> table
     * reference
     */
    public PublicTransportStop(Name alias) {
        this(alias, PUBLIC_TRANSPORT_STOP);
    }

    /**
     * Create a <code>routing.public_transport_stop</code> table reference
     */
    public PublicTransportStop() {
        this(DSL.name("public_transport_stop"), null);
    }

    public <O extends Record> PublicTransportStop(Table<O> path, ForeignKey<O, PublicTransportStopRecord> childPath, InverseForeignKey<O, PublicTransportStopRecord> parentPath) {
        super(path, childPath, parentPath, PUBLIC_TRANSPORT_STOP);
    }

    /**
     * A subtype implementing {@link Path} for simplified path-based joins.
     */
    public static class PublicTransportStopPath extends PublicTransportStop implements Path<PublicTransportStopRecord> {

        private static final long serialVersionUID = 1L;
        public <O extends Record> PublicTransportStopPath(Table<O> path, ForeignKey<O, PublicTransportStopRecord> childPath, InverseForeignKey<O, PublicTransportStopRecord> parentPath) {
            super(path, childPath, parentPath);
        }
        private PublicTransportStopPath(Name alias, Table<PublicTransportStopRecord> aliased) {
            super(alias, aliased);
        }

        @Override
        public PublicTransportStopPath as(String alias) {
            return new PublicTransportStopPath(DSL.name(alias), this);
        }

        @Override
        public PublicTransportStopPath as(Name alias) {
            return new PublicTransportStopPath(alias, this);
        }

        @Override
        public PublicTransportStopPath as(Table<?> alias) {
            return new PublicTransportStopPath(alias.getQualifiedName(), this);
        }
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Routing.ROUTING;
    }

    @Override
    public UniqueKey<PublicTransportStopRecord> getPrimaryKey() {
        return Keys.PUBLIC_TRANSPORT_STOP_PKEY;
    }

    @Override
    public List<ForeignKey<PublicTransportStopRecord, ?>> getReferences() {
        return Arrays.asList(Keys.PUBLIC_TRANSPORT_STOP__PUBLIC_TRANSPORT_STOP_INFRASTRUCTURE_LINK_FKEY, Keys.PUBLIC_TRANSPORT_STOP__PUBLIC_TRANSPORT_STOP_INFRASTRUCTURE_SOURCE_FKEY);
    }

    private transient InfrastructureLinkPath _infrastructureLink;

    /**
     * Get the implicit join path to the
     * <code>routing.infrastructure_link</code> table.
     */
    public InfrastructureLinkPath infrastructureLink() {
        if (_infrastructureLink == null)
            _infrastructureLink = new InfrastructureLinkPath(this, Keys.PUBLIC_TRANSPORT_STOP__PUBLIC_TRANSPORT_STOP_INFRASTRUCTURE_LINK_FKEY, null);

        return _infrastructureLink;
    }

    private transient InfrastructureSourcePath _infrastructureSource;

    /**
     * Get the implicit join path to the
     * <code>routing.infrastructure_source</code> table.
     */
    public InfrastructureSourcePath infrastructureSource() {
        if (_infrastructureSource == null)
            _infrastructureSource = new InfrastructureSourcePath(this, Keys.PUBLIC_TRANSPORT_STOP__PUBLIC_TRANSPORT_STOP_INFRASTRUCTURE_SOURCE_FKEY, null);

        return _infrastructureSource;
    }

    @Override
    public PublicTransportStop as(String alias) {
        return new PublicTransportStop(DSL.name(alias), this);
    }

    @Override
    public PublicTransportStop as(Name alias) {
        return new PublicTransportStop(alias, this);
    }

    @Override
    public PublicTransportStop as(Table<?> alias) {
        return new PublicTransportStop(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public PublicTransportStop rename(String name) {
        return new PublicTransportStop(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public PublicTransportStop rename(Name name) {
        return new PublicTransportStop(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public PublicTransportStop rename(Table<?> name) {
        return new PublicTransportStop(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public PublicTransportStop where(Condition condition) {
        return new PublicTransportStop(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public PublicTransportStop where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public PublicTransportStop where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public PublicTransportStop where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public PublicTransportStop where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public PublicTransportStop where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public PublicTransportStop where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public PublicTransportStop where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public PublicTransportStop whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public PublicTransportStop whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
