package me.blvckbytes.simmodemapi.modem

class SimModemCommand(
  val command: String,
  val timeoutMs: Int,
  val responsePredicate: ResponsePredicate?
)