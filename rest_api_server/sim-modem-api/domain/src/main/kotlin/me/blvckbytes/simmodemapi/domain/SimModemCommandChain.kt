package me.blvckbytes.simmodemapi.domain

class SimModemCommandChain(
  val commands: List<SimModemCommand>,
  val resultHandler: SimModemResultHandler
)