package me.blvckbytes.simmodemapi.rest.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import me.blvckbytes.simmodemapi.domain.ValidityPeriodUnit

@ValidValidityPeriod
class SendSmsRequestDto(
  @field:NotEmpty
  @field:Pattern(regexp = "\\+43\\d{3,}")
  val recipient: String?,

  @field:NotEmpty
  @field:Size(min=1)
  val message: String?,

  override val validityPeriodUnit: ValidityPeriodUnit?,
  override val validityPeriodValue: Double?,
) : ValidityPeriodContainer