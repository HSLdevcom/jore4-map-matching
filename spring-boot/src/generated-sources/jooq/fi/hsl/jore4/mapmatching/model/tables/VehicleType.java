/*
 * This file is generated by jOOQ.
 */
package fi.hsl.jore4.mapmatching.model.tables;


import fi.hsl.jore4.mapmatching.model.Keys;
import fi.hsl.jore4.mapmatching.model.Routing;
import fi.hsl.jore4.mapmatching.model.tables.records.VehicleTypeRecord;

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
 * The vehicle types from Transmodel: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:9:360
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class VehicleType extends TableImpl<VehicleTypeRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>routing.vehicle_type</code>
     */
    public static final VehicleType VEHICLE_TYPE = new VehicleType();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<VehicleTypeRecord> getRecordType() {
        return VehicleTypeRecord.class;
    }

    /**
     * The column <code>routing.vehicle_type.vehicle_type</code>. The vehicle type from Transmodel: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:9:360
     */
    public final TableField<VehicleTypeRecord, String> VEHICLE_TYPE_ = createField(DSL.name("vehicle_type"), SQLDataType.CLOB.nullable(false), this, "The vehicle type from Transmodel: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:9:360");

    /**
     * The column <code>routing.vehicle_type.belonging_to_vehicle_mode</code>. The vehicle mode the vehicle type belongs to: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:1:283
     */
    public final TableField<VehicleTypeRecord, String> BELONGING_TO_VEHICLE_MODE = createField(DSL.name("belonging_to_vehicle_mode"), SQLDataType.CLOB.nullable(false), this, "The vehicle mode the vehicle type belongs to: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:1:283");

    private VehicleType(Name alias, Table<VehicleTypeRecord> aliased) {
        this(alias, aliased, null);
    }

    private VehicleType(Name alias, Table<VehicleTypeRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment("The vehicle types from Transmodel: https://www.transmodel-cen.eu/model/index.htm?goto=1:6:9:360"), TableOptions.table());
    }

    /**
     * Create an aliased <code>routing.vehicle_type</code> table reference
     */
    public VehicleType(String alias) {
        this(DSL.name(alias), VEHICLE_TYPE);
    }

    /**
     * Create an aliased <code>routing.vehicle_type</code> table reference
     */
    public VehicleType(Name alias) {
        this(alias, VEHICLE_TYPE);
    }

    /**
     * Create a <code>routing.vehicle_type</code> table reference
     */
    public VehicleType() {
        this(DSL.name("vehicle_type"), null);
    }

    public <O extends Record> VehicleType(Table<O> child, ForeignKey<O, VehicleTypeRecord> key) {
        super(child, key, VEHICLE_TYPE);
    }

    @Override
    public Schema getSchema() {
        return Routing.ROUTING;
    }

    @Override
    public UniqueKey<VehicleTypeRecord> getPrimaryKey() {
        return Keys.VEHICLE_TYPE_PKEY;
    }

    @Override
    public List<UniqueKey<VehicleTypeRecord>> getKeys() {
        return Arrays.<UniqueKey<VehicleTypeRecord>>asList(Keys.VEHICLE_TYPE_PKEY);
    }

    @Override
    public List<ForeignKey<VehicleTypeRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<VehicleTypeRecord, ?>>asList(Keys.VEHICLE_TYPE__VEHICLE_TYPE_BELONGING_TO_VEHICLE_MODE_FKEY);
    }

    private transient VehicleMode _vehicleMode;

    public VehicleMode vehicleMode() {
        if (_vehicleMode == null)
            _vehicleMode = new VehicleMode(this, Keys.VEHICLE_TYPE__VEHICLE_TYPE_BELONGING_TO_VEHICLE_MODE_FKEY);

        return _vehicleMode;
    }

    @Override
    public VehicleType as(String alias) {
        return new VehicleType(DSL.name(alias), this);
    }

    @Override
    public VehicleType as(Name alias) {
        return new VehicleType(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public VehicleType rename(String name) {
        return new VehicleType(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public VehicleType rename(Name name) {
        return new VehicleType(name, null);
    }

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row2<String, String> fieldsRow() {
        return (Row2) super.fieldsRow();
    }
}
