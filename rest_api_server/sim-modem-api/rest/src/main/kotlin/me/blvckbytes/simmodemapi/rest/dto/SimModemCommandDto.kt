package me.blvckbytes.simmodemapi.rest.dto

import me.blvckbytes.simmodemapi.domain.BinaryUtils
import me.blvckbytes.simmodemapi.domain.SimModemResponse
import java.time.LocalDateTime

class SimModemCommandDto(
  val readableCommand: String,
  val binaryCommand: String,
  val readableResponse: String,
  val binaryResponse: String,
  val timeoutMs: Int,
  val commandSentStamp: LocalDateTime,
  val responseReceivedStamp: LocalDateTime
) {
  companion object {
    fun fromModel(model: SimModemResponse): SimModemCommandDto {
      return SimModemCommandDto(
        model.command.readableCommand,
        BinaryUtils.binaryToHexString(model.command.binaryCommand),
        model.readableContent,
        BinaryUtils.binaryToHexString(model.binaryContent),
        model.command.customTimeoutMs ?: model.command.type.timeoutMs,
        model.commandSentStamp,
        model.responseReceivedStamp
      )
    }
  }
}