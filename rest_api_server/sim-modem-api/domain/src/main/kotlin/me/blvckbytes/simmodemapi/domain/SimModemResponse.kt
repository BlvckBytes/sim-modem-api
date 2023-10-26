package me.blvckbytes.simmodemapi.domain

import java.time.LocalDateTime

class SimModemResponse(
  val binaryContent: ByteArray,
  val readableContent: String,
  val command: SimModemCommand,
  val commandSentStamp: LocalDateTime,
  val responseReceivedStamp: LocalDateTime
)