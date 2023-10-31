package me.blvckbytes.simmodemapi.domain

class SimModemCommand(
  val type: SimModemCommandType,
  val binaryCommand: ByteArray,
  val readableCommand: String,
  val customTimeoutMs: Int?,
  val responsePredicate: ResponsePredicate?
)