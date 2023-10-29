package me.blvckbytes.simmodemapi.rest.dto

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class ValidBinaryCommandValidator : ConstraintValidator<ValidBinaryCommand, String> {

  override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
    context!!.disableDefaultConstraintViolation()

    // NOTE: value cannot be null or blank, since @NotBlank is applied beforehand (see @ValidBinaryCommand)

    if (value!!.length % 2 != 0) {
      context.buildConstraintViolationWithTemplate(
        "Has to be of even length"
      ).addConstraintViolation()
      return false
    }

    if (value.contains(" ")) {
      context.buildConstraintViolationWithTemplate(
        "Cannot contain spaces"
      ).addConstraintViolation()
      return false
    }

    return true
  }
}