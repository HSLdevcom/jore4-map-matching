package fi.hsl.jore4.mapmatching.test.generators

import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate
import org.quicktheories.generators.SourceDSL

object CommonGenerators {

    val BOOLEAN: Gen<Boolean> = Generate.booleans()

    val ZERO_DOUBLE: Gen<Double> = SourceDSL.doubles().between(0.0, 0.0)
}
