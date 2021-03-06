/*
 * This file is generated by jOOQ.
 */
package fi.hsl.jore4.mapmatching.model.tables;


import fi.hsl.jore4.mapmatching.model.Keys;
import fi.hsl.jore4.mapmatching.model.Routing;
import fi.hsl.jore4.mapmatching.model.tables.records.InfrastructureLinkSafelyTraversedByVehicleTypeRecord;

import java.util.Arrays;
import java.util.List;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row2;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * Which infrastructure links are safely traversed by which vehicle types?
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InfrastructureLinkSafelyTraversedByVehicleType extends TableImpl<InfrastructureLinkSafelyTraversedByVehicleTypeRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>routing.infrastructure_link_safely_traversed_by_vehicle_type</code>
     */
    public static final InfrastructureLinkSafelyTraversedByVehicleType INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE = new InfrastructureLinkSafelyTraversedByVehicleType();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<InfrastructureLinkSafelyTraversedByVehicleTypeRecord> getRecordType() {
        return InfrastructureLinkSafelyTraversedByVehicleTypeRecord.class;
    }

    /**
     * The column <code>routing.infrastructure_link_safely_traversed_by_vehicle_type.infrastructure_link_id</code>. The infrastructure link that can be safely traversed by the vehicle type
     */
    public final TableField<InfrastructureLinkSafelyTraversedByVehicleTypeRecord, Long> INFRASTRUCTURE_LINK_ID = createField(DSL.name("infrastructure_link_id"), SQLDataType.BIGINT.nullable(false), this, "The infrastructure link that can be safely traversed by the vehicle type");

    /**
     * The column <code>routing.infrastructure_link_safely_traversed_by_vehicle_type.vehicle_type</code>. The vehicle type that can safely traverse the infrastructure link
     */
    public final TableField<InfrastructureLinkSafelyTraversedByVehicleTypeRecord, String> VEHICLE_TYPE = createField(DSL.name("vehicle_type"), SQLDataType.CLOB.nullable(false), this, "The vehicle type that can safely traverse the infrastructure link");

    private InfrastructureLinkSafelyTraversedByVehicleType(Name alias, Table<InfrastructureLinkSafelyTraversedByVehicleTypeRecord> aliased) {
        this(alias, aliased, null);
    }

    private InfrastructureLinkSafelyTraversedByVehicleType(Name alias, Table<InfrastructureLinkSafelyTraversedByVehicleTypeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment("Which infrastructure links are safely traversed by which vehicle types?"), TableOptions.table());
    }

    /**
     * Create an aliased <code>routing.infrastructure_link_safely_traversed_by_vehicle_type</code> table reference
     */
    public InfrastructureLinkSafelyTraversedByVehicleType(String alias) {
        this(DSL.name(alias), INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE);
    }

    /**
     * Create an aliased <code>routing.infrastructure_link_safely_traversed_by_vehicle_type</code> table reference
     */
    public InfrastructureLinkSafelyTraversedByVehicleType(Name alias) {
        this(alias, INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE);
    }

    /**
     * Create a <code>routing.infrastructure_link_safely_traversed_by_vehicle_type</code> table reference
     */
    public InfrastructureLinkSafelyTraversedByVehicleType() {
        this(DSL.name("infrastructure_link_safely_traversed_by_vehicle_type"), null);
    }

    public <O extends Record> InfrastructureLinkSafelyTraversedByVehicleType(Table<O> child, ForeignKey<O, InfrastructureLinkSafelyTraversedByVehicleTypeRecord> key) {
        super(child, key, INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE);
    }

    @Override
    public Schema getSchema() {
        return Routing.ROUTING;
    }

    @Override
    public UniqueKey<InfrastructureLinkSafelyTraversedByVehicleTypeRecord> getPrimaryKey() {
        return Keys.INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE_PKEY;
    }

    @Override
    public List<UniqueKey<InfrastructureLinkSafelyTraversedByVehicleTypeRecord>> getKeys() {
        return Arrays.<UniqueKey<InfrastructureLinkSafelyTraversedByVehicleTypeRecord>>asList(Keys.INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE_PKEY);
    }

    @Override
    public List<ForeignKey<InfrastructureLinkSafelyTraversedByVehicleTypeRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<InfrastructureLinkSafelyTraversedByVehicleTypeRecord, ?>>asList(Keys.INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE__INFRASTRUCTURE_LINK_SAFELY_TRAVERSE_INFRASTRUCTURE_LINK_ID_FKEY, Keys.INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE__INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHIC_VEHICLE_TYPE_FKEY);
    }

    private transient InfrastructureLink _infrastructureLink;
    private transient VehicleType _vehicleType;

    public InfrastructureLink infrastructureLink() {
        if (_infrastructureLink == null)
            _infrastructureLink = new InfrastructureLink(this, Keys.INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE__INFRASTRUCTURE_LINK_SAFELY_TRAVERSE_INFRASTRUCTURE_LINK_ID_FKEY);

        return _infrastructureLink;
    }

    public VehicleType vehicleType() {
        if (_vehicleType == null)
            _vehicleType = new VehicleType(this, Keys.INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHICLE_TYPE__INFRASTRUCTURE_LINK_SAFELY_TRAVERSED_BY_VEHIC_VEHICLE_TYPE_FKEY);

        return _vehicleType;
    }

    @Override
    public InfrastructureLinkSafelyTraversedByVehicleType as(String alias) {
        return new InfrastructureLinkSafelyTraversedByVehicleType(DSL.name(alias), this);
    }

    @Override
    public InfrastructureLinkSafelyTraversedByVehicleType as(Name alias) {
        return new InfrastructureLinkSafelyTraversedByVehicleType(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public InfrastructureLinkSafelyTraversedByVehicleType rename(String name) {
        return new InfrastructureLinkSafelyTraversedByVehicleType(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public InfrastructureLinkSafelyTraversedByVehicleType rename(Name name) {
        return new InfrastructureLinkSafelyTraversedByVehicleType(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<Long, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
