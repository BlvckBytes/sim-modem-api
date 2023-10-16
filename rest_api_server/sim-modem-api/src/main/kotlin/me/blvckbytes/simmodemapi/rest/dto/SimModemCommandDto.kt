package me.blvckbytes.simmodemapi.rest.dto

import me.blvckbytes.simmodemapi.modem.SimModemResponse
import me.blvckbytes.simmodemapi.rest.CommandGeneratorPort
import java.time.LocalDateTime

class SimModemCommandDto(
  val command: String,
  val response: String,
  val timeoutMs: Int,
  val commandSentStamp: LocalDateTime,
  val responseReceivedStamp: LocalDateTime
) {
  companion object {
    fun fromModel(model: SimModemResponse, commandGenerator: CommandGeneratorPort): SimModemCommandDto {
      return SimModemCommandDto(
        // TODO: Also substitute non printable characters
        commandGenerator.trimControlCharacters(model.command.command),
        commandGenerator.trimControlCharacters(model.content),
        model.command.timeoutMs,
        model.commandSentStamp,
        model.responseReceivedStamp
      )
    }
  }
}