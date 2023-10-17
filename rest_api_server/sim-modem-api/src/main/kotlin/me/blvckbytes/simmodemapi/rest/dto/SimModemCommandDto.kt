package me.blvckbytes.simmodemapi.rest.dto

import me.blvckbytes.simmodemapi.modem.CommandGeneratorAdapter
import me.blvckbytes.simmodemapi.modem.SimModemResponse
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
        CommandGeneratorAdapter.binaryToHexString(model.command.binaryCommand),
        model.readableContent,
        CommandGeneratorAdapter.binaryToHexString(model.binaryContent),
        model.command.timeoutMs,
        model.commandSentStamp,
        model.responseReceivedStamp
      )
    }
  }
}