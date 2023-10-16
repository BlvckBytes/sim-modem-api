package me.blvckbytes.simmodemapi.modem

import java.time.LocalDateTime

class SimModemResponse(
  val binaryContent: ByteArray,
  val readableContent: String,
  val command: SimModemCommand,
  val commandSentStamp: LocalDateTime,
  val responseReceivedStamp: LocalDateTime
)