package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.jooq.Binding
import org.jooq.BindingGetResultSetContext
import org.jooq.BindingGetSQLInputContext
import org.jooq.BindingGetStatementContext
import org.jooq.BindingRegisterContext
import org.jooq.BindingSQLContext
import org.jooq.BindingSetSQLOutputContext
import org.jooq.BindingSetStatementContext
import org.jooq.Converter
import org.jooq.impl.DSL
import org.locationtech.jts.geom.LineString
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.Types
import java.util.Objects

class LineStringBinding : Binding<Any, LineString> {

    override fun converter(): Converter<Any, LineString> = LineStringConverter.INSTANCE

    @Throws(SQLException::class)
    override fun sql(ctx: BindingSQLContext<LineString>) {
        ctx.render().visit(DSL.sql("?::geometry"))
    }

    @Throws(SQLException::class)
    override fun register(ctx: BindingRegisterContext<LineString>) {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR)
    }

    @Throws(SQLException::class)
    override fun set(ctx: BindingSetStatementContext<LineString>) {
        ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null))
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetResultSetContext<LineString>) {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()))
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetStatementContext<LineString>) {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()))
    }

    @Throws(SQLException::class)
    override fun set(ctx: BindingSetSQLOutputContext<LineString>) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetSQLInputContext<LineString>) {
        throw SQLFeatureNotSupportedException()
    }
}
