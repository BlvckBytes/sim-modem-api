package me.blvckbytes.simmodemapi.modem

import java.time.LocalDateTime

class SimModemResponse(
  val content: String,
  val command: SimModemCommand,
  val commandSentStamp: LocalDateTime,
  val responseReceivedStamp: LocalDateTime
)