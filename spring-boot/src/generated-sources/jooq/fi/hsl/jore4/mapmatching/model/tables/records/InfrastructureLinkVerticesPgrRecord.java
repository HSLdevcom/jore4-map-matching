/*
 * This file is generated by jOOQ.
 */
package fi.hsl.jore4.mapmatching.model.tables.records;


import fi.hsl.jore4.mapmatching.model.tables.InfrastructureLinkVerticesPgr;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Row6;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * Topology nodes created for infrastructure links by pgRougting
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class InfrastructureLinkVerticesPgrRecord extends UpdatableRecordImpl<InfrastructureLinkVerticesPgrRecord> implements Record6<Long, Integer, Integer, Integer, Integer, Object> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>routing.infrastructure_link_vertices_pgr.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link_vertices_pgr.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>routing.infrastructure_link_vertices_pgr.cnt</code>.
     */
    public void setCnt(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link_vertices_pgr.cnt</code>.
     */
    public Integer getCnt() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>routing.infrastructure_link_vertices_pgr.chk</code>.
     */
    public void setChk(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link_vertices_pgr.chk</code>.
     */
    public Integer getChk() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>routing.infrastructure_link_vertices_pgr.ein</code>.
     */
    public void setEin(Integer value) {
        set(3, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link_vertices_pgr.ein</code>.
     */
    public Integer getEin() {
        return (Integer) get(3);
    }

    /**
     * Setter for <code>routing.infrastructure_link_vertices_pgr.eout</code>.
     */
    public void setEout(Integer value) {
        set(4, value);
    }

    /**
     * Getter for <code>routing.infrastructure_link_vertices_pgr.eout</code>.
     */
    public Integer getEout() {
        return (Integer) get(4);
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    public void setTheGeom(Object value) {
        set(5, value);
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    public Object getTheGeom() {
        return get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record6 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row6<Long, Integer, Integer, Integer, Integer, Object> fieldsRow() {
        return (Row6) super.fieldsRow();
    }

    @Override
    public Row6<Long, Integer, Integer, Integer, Integer, Object> valuesRow() {
        return (Row6) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return InfrastructureLinkVerticesPgr.INFRASTRUCTURE_LINK_VERTICES_PGR.ID;
    }

    @Override
    public Field<Integer> field2() {
        return InfrastructureLinkVerticesPgr.INFRASTRUCTURE_LINK_VERTICES_PGR.CNT;
    }

    @Override
    public Field<Integer> field3() {
        return InfrastructureLinkVerticesPgr.INFRASTRUCTURE_LINK_VERTICES_PGR.CHK;
    }

    @Override
    public Field<Integer> field4() {
        return InfrastructureLinkVerticesPgr.INFRASTRUCTURE_LINK_VERTICES_PGR.EIN;
    }

    @Override
    public Field<Integer> field5() {
        return InfrastructureLinkVerticesPgr.INFRASTRUCTURE_LINK_VERTICES_PGR.EOUT;
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    @Override
    public Field<Object> field6() {
        return InfrastructureLinkVerticesPgr.INFRASTRUCTURE_LINK_VERTICES_PGR.THE_GEOM;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getCnt();
    }

    @Override
    public Integer component3() {
        return getChk();
    }

    @Override
    public Integer component4() {
        return getEin();
    }

    @Override
    public Integer component5() {
        return getEout();
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    @Override
    public Object component6() {
        return getTheGeom();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getCnt();
    }

    @Override
    public Integer value3() {
        return getChk();
    }

    @Override
    public Integer value4() {
        return getEin();
    }

    @Override
    public Integer value5() {
        return getEout();
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    @Override
    public Object value6() {
        return getTheGeom();
    }

    @Override
    public InfrastructureLinkVerticesPgrRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public InfrastructureLinkVerticesPgrRecord value2(Integer value) {
        setCnt(value);
        return this;
    }

    @Override
    public InfrastructureLinkVerticesPgrRecord value3(Integer value) {
        setChk(value);
        return this;
    }

    @Override
    public InfrastructureLinkVerticesPgrRecord value4(Integer value) {
        setEin(value);
        return this;
    }

    @Override
    public InfrastructureLinkVerticesPgrRecord value5(Integer value) {
        setEout(value);
        return this;
    }

    /**
     * @deprecated Unknown data type. Please define an explicit {@link org.jooq.Binding} to specify how this type should be handled. Deprecation can be turned off using {@literal <deprecationOnUnknownTypes/>} in your code generator configuration.
     */
    @Deprecated
    @Override
    public InfrastructureLinkVerticesPgrRecord value6(Object value) {
        setTheGeom(value);
        return this;
    }

    @Override
    public InfrastructureLinkVerticesPgrRecord values(Long value1, Integer value2, Integer value3, Integer value4, Integer value5, Object value6) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached InfrastructureLinkVerticesPgrRecord
     */
    public InfrastructureLinkVerticesPgrRecord() {
        super(InfrastructureLinkVerticesPgr.INFRASTRUCTURE_LINK_VERTICES_PGR);
    }

    /**
     * Create a detached, initialised InfrastructureLinkVerticesPgrRecord
     */
    public InfrastructureLinkVerticesPgrRecord(Long id, Integer cnt, Integer chk, Integer ein, Integer eout, Object theGeom) {
        super(InfrastructureLinkVerticesPgr.INFRASTRUCTURE_LINK_VERTICES_PGR);

        setId(id);
        setCnt(cnt);
        setChk(chk);
        setEin(ein);
        setEout(eout);
        setTheGeom(theGeom);
    }
}