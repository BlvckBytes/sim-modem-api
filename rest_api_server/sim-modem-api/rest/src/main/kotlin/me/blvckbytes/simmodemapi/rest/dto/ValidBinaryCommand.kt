package me.blvckbytes.simmodemapi.rest.dto

import jakarta.validation.Constraint
import jakarta.validation.constraints.NotBlank
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@NotBlank
@Constraint(validatedBy = [ValidBinaryCommandValidator::class])
annotation class ValidBinaryCommand(
  val message: String = "{jakarta.validation.constraints.ValidBinaryCommand.message}",
  val groups: Array<KClass<Any>> = [],
  val payload: Array<KClass<*>> = []
)