package me.blvckbytes.simmodemapi.rest.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import me.blvckbytes.simmodemapi.domain.SimModemCommand

class CustomCommandDto(
  @field:NotBlank
  val binaryCommand: String?,
  @field:NotBlank
  val readableCommand: String?,
  @field:NotNull
  val timeoutMs: Int?
) {
  companion object {
    private fun parseBytes(container: String): ByteArray {
      // TODO: Throw proper exception
      if (container.length % 2 != 0 || container.contains(" "))
        throw IllegalStateException("Bytes should be notated as two hex chars each, without spaces")

      return container
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
    }
  }

  fun toCommand(): SimModemCommand {
    return SimModemCommand(
      parseBytes(binaryCommand!!),
      readableCommand!!,
      timeoutMs!!,
      null
    )
  }
}