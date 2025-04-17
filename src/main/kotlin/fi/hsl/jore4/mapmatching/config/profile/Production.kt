package fi.hsl.jore4.mapmatching.config.profile

import org.springframework.context.annotation.Profile

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention
@Profile(SpringProfiles.PRODUCTION)
annotation class Production
