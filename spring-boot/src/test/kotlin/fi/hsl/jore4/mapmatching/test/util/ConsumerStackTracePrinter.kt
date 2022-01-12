package fi.hsl.jore4.mapmatching.test.util

import java.util.function.Consumer

class ConsumerStackTracePrinter<T>(private val delegate: Consumer<T>) : Consumer<T> {
    override fun accept(value: T) {
        try {
            delegate.accept(value)
        } catch (ex: RuntimeException) {
            ex.printStackTrace()
            throw ex
        }
    }
}
