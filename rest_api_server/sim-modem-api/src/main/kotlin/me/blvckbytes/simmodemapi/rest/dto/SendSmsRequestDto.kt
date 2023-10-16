package me.blvckbytes.simmodemapi.rest.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

class SendSmsRequestDto(
  @field:NotEmpty
  @field:Pattern(regexp = "\\+43\\d{3,}")
  val recipient: String?,

  // TODO: More granular validation
  // A single SMS message technically supports up to 160 characters, or up to 70 if the message
  // contains one or more Unicode characters (such as emoji or Chinese characters).
  @field:NotEmpty
  @field:Size(min=1, max=160)
  val message: String?
) {
}