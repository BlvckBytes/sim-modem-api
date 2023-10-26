package me.blvckbytes.simmodemapi.rest.dto

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidValidityPeriodValidator : ConstraintValidator<ValidValidityPeriod, ValidityPeriodContainer> {

  override fun isValid(value: ValidityPeriodContainer?, context: ConstraintValidatorContext?): Boolean {
    if (value !is ValidityPeriodContainer)
      throw IllegalStateException("Requested value was not of expected type")

    val validityPeriodUnit = value.validityPeriodUnit ?: return true
    val validityPeriodValue = value.validityPeriodValue

    context!!.disableDefaultConstraintViolation()

    if (validityPeriodValue == null) {
      context.buildConstraintViolationWithTemplate(
        "Required, since 'validityPeriodUnit' has been provided"
      ).addPropertyNode("validityPeriodValue").addConstraintViolation()
      return false
    }

    if (validityPeriodValue < validityPeriodUnit.min || validityPeriodValue > validityPeriodUnit.max) {
      context.buildConstraintViolationWithTemplate(
        "Has to be within the range of ${validityPeriodUnit.name}: ${validityPeriodUnit.min} < validityPeriodValue < ${validityPeriodUnit.max}"
      ).addPropertyNode("validityPeriodValue").addConstraintViolation()
      return false
    }

    if (validityPeriodValue % validityPeriodUnit.step != 0.0) {
      context.buildConstraintViolationWithTemplate(
        "Has to be a multiple of ${validityPeriodUnit.name}'s step-size: ${validityPeriodUnit.step}"
      ).addPropertyNode("validityPeriodValue").addConstraintViolation()
      return false
    }

    return true
  }
}