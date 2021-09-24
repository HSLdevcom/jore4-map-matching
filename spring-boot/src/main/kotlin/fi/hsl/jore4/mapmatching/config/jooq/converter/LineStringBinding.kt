package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.geolatte.geom.C2D
import org.geolatte.geom.LineString
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
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.Types
import java.util.Objects

class LineStringBinding : Binding<Any, LineString<C2D>> {

    override fun converter(): Converter<Any, LineString<C2D>> = LineStringConverter.INSTANCE

    @Throws(SQLException::class)
    override fun sql(ctx: BindingSQLContext<LineString<C2D>>) {
        ctx.render().visit(DSL.sql("?::geometry"))
    }

    @Throws(SQLException::class)
    override fun register(ctx: BindingRegisterContext<LineString<C2D>>) {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR)
    }

    @Throws(SQLException::class)
    override fun set(ctx: BindingSetStatementContext<LineString<C2D>>) {
        ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null))
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetResultSetContext<LineString<C2D>>) {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()))
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetStatementContext<LineString<C2D>>) {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()))
    }

    @Throws(SQLException::class)
    override fun set(ctx: BindingSetSQLOutputContext<LineString<C2D>>) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetSQLInputContext<LineString<C2D>>) {
        throw SQLFeatureNotSupportedException()
    }
}
