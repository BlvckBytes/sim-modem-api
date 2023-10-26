package me.blvckbytes.simmodemapi.domain

class SimModemCommandChain(
  val type: CommandChainType,
  val commands: List<SimModemCommand>,
  val resultHandler: SimModemResultHandler
)