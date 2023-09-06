package fi.hsl.jore4.mapmatching.test.generators

import org.quicktheories.core.Gen
import org.quicktheories.core.RandomnessSource
import java.util.function.Predicate

/**
 * This is used to implement unique value generation.
 *
 * This is slightly modified (Kotlin->Java) version of internal QuickTheories class that is not exposed to public API.
 *
 * The original source code file (https://github.com/quicktheories/QuickTheories/blob/master/core/src/main/java/org/quicktheories/generators/CodePoints.java)
 * is licensed by https://github.com/quicktheories/QuickTheories under Apace License V2.0.
 */
class Retry<T : Any>(private val child: Gen<T>, private val assumption: Predicate<T>) : Gen<T> {

    override fun generate(randomnessSource: RandomnessSource): T {
        // danger of infinite loop if used incorrectly
        while (true) {
            val detached = randomnessSource.detach()
            val t = child.generate(detached)

            if (assumption.test(t)) {
                detached.commit()
                return t
            }
        }
    }

    override fun asString(t: T): String = child.asString(t)
}
