package me.blvckbytes.simmodemapi.domain

class SimModemCommand(
  val binaryCommand: ByteArray,
  val readableCommand: String,
  val timeoutMs: Int,
  val responsePredicate: ResponsePredicate?
)