package me.blvckbytes.simmodemapi.modem

class SimModemCommand(
  val binaryCommand: ByteArray,
  val readableCommand: String,
  val timeoutMs: Int,
  val responsePredicate: ResponsePredicate?
)