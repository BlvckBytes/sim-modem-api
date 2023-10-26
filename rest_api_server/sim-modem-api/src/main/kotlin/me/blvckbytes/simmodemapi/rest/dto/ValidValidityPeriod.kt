package me.blvckbytes.simmodemapi.rest.dto

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ValidValidityPeriodValidator::class])
annotation class ValidValidityPeriod(
  val message: String = "{jakarta.validation.constraints.ValidValidityPeriod.message}",
  val groups: Array<KClass<Any>> = [],
  val payload: Array<KClass<*>> = []
)