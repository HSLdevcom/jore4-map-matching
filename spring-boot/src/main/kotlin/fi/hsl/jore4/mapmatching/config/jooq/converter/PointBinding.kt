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
import org.locationtech.jts.geom.Point
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.Types
import java.util.Objects

class PointBinding : Binding<Any, Point> {

    override fun converter(): Converter<Any, Point> = PointConverter.INSTANCE

    @Throws(SQLException::class)
    override fun sql(ctx: BindingSQLContext<Point>) {
        ctx.render().visit(DSL.sql("?::geometry"))
    }

    @Throws(SQLException::class)
    override fun register(ctx: BindingRegisterContext<Point>) {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR)
    }

    @Throws(SQLException::class)
    override fun set(ctx: BindingSetStatementContext<Point>) {
        ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null))
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetResultSetContext<Point>) {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()))
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetStatementContext<Point>) {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()))
    }

    @Throws(SQLException::class)
    override fun set(ctx: BindingSetSQLOutputContext<Point>) {
        throw SQLFeatureNotSupportedException()
    }

    @Throws(SQLException::class)
    override fun get(ctx: BindingGetSQLInputContext<Point>) {
        throw SQLFeatureNotSupportedException()
    }
}
