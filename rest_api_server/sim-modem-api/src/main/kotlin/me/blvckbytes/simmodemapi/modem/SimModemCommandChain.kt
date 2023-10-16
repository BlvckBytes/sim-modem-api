package me.blvckbytes.simmodemapi.modem

class SimModemCommandChain(
  val type: CommandChainType,
  val commands: List<SimModemCommand>,
  val resultHandler: SimModemResultHandler
)