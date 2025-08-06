package fi.hsl.jore4.mapmatching.util

import org.springframework.stereotype.Component

/**
 * A meta-annotation for Spring's [Component] annotation. Marks a Spring-managed
 * component to be used internally in the service layer. A class marked with
 * this annotation should not be directly invoked from the web layer (a Spring
 * controller class).
 *
 * Internal service methods should not mark database transactions to be rolled
 * back in case of [RuntimeException]s. This is because the actual service
 * methods that open database transactions are enabled to recover from an
 * exception thrown from an internal service method.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class InternalService
