package me.blvckbytes.simmodemapi.rest.dto

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
    private fun binaryToHexString(buffer: ByteArray): String {
      val result = StringBuilder()
      for (byte in buffer)
        result.append("%02X".format(byte))
      return result.toString()
    }

    fun fromModel(model: SimModemResponse): SimModemCommandDto {
      return SimModemCommandDto(
        model.command.readableCommand,
        binaryToHexString(model.command.binaryCommand),
        model.readableContent,
        binaryToHexString(model.binaryContent),
        model.command.timeoutMs,
        model.commandSentStamp,
        model.responseReceivedStamp
      )
    }
  }
}