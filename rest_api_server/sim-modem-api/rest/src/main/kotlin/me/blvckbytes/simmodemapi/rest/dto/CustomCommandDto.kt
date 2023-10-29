package me.blvckbytes.simmodemapi.rest.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import me.blvckbytes.simmodemapi.domain.SimModemCommand

class CustomCommandDto(
  @field:ValidBinaryCommand
  val binaryCommand: String?,
  @field:NotBlank
  val readableCommand: String?,
  @field:NotNull
  val timeoutMs: Int?
) {
  fun toCommand(): SimModemCommand {
    val bytes = binaryCommand!!
      .chunked(2)
      .map { it.toInt(16).toByte() }
      .toByteArray()

    return SimModemCommand(
      bytes,
      readableCommand!!,
      timeoutMs!!,
      null
    )
  }
}