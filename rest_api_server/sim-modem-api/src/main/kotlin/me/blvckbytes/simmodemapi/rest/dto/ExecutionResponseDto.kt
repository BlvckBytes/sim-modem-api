package me.blvckbytes.simmodemapi.rest.dto

import me.blvckbytes.simmodemapi.modem.ExecutionResult

class ExecutionResponseDto(
  val result: ExecutionResult,
  val executedCommands: List<SimModemCommandDto>
)