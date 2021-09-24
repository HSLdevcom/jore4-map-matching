package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.geolatte.geom.C2D
import org.geolatte.geom.Point
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

class PointBinding : Binding<Any, Point<C2D>> {

    override fun converter(): Converter<Any, Point<C2D>> = PointConverter.INSTANCE

    @Throws(SQLException::class)
    override fun sql(ctx: BindingSQLContext<Point<C2D>>) {
        ctx.render().visit(DSL.sql("?::geometry"))
    }

    @Throws(SQLException::class)
    override fun register(ctx: BindingRegisterContext<Point<C2D>>) {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR)
    }

    @Throws(SQLException::class)
    override fun set(ctx: BindingSetStatementContext<Point<C2D>>) {
        ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null))
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetResultSetContext<Point<C2D>>) {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()))
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetStatementContext<Point<C2D>>) {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()))
    }

    @Throws(SQLException::class)
    override fun set(ctx: BindingSetSQLOutputContext<Point<C2D>>) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetSQLInputContext<Point<C2D>>) {
        throw SQLFeatureNotSupportedException()
    }
}
