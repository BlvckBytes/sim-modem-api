package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.ExecutionResult

class ExecutionResponseDto(
  val result: ExecutionResult,
  val executedCommands: List<SimModemCommandDto>
)