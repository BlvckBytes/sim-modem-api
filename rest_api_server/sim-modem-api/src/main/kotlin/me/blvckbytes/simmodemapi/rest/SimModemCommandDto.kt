package me.blvckbytes.simmodemapi.rest

import com.fasterxml.jackson.annotation.JsonFormat
import me.blvckbytes.simmodemapi.modem.SimModemResponse
import java.time.LocalDateTime

class SimModemCommandDto(
  val command: String,
  val response: String,
  val timeoutMs: Int,
//  @field:JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
  val commandSentStamp: LocalDateTime,
//  @field:JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
  val responseReceivedStamp: LocalDateTime
) {
  companion object {
    fun fromModel(model: SimModemResponse, commandGenerator: CommandGeneratorPort): SimModemCommandDto {
      return SimModemCommandDto(
        commandGenerator.trimControlCharacters(model.command.command),
        commandGenerator.trimControlCharacters(model.content),
        model.command.timeoutMs,
        model.commandSentStamp,
        model.responseReceivedStamp
      )
    }
  }
}