package me.blvckbytes.simmodemapi.rest.controller

import jakarta.validation.Valid
import me.blvckbytes.simmodemapi.rest.SimModemService
import me.blvckbytes.simmodemapi.rest.dto.CustomCommandDto
import me.blvckbytes.simmodemapi.rest.dto.ExecutionResponseDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult

@RestController
@RequestMapping("custom-command")
class CustomCommandController(
  val simModemService: SimModemService
) {

  @PostMapping
  fun executeCustomCommand(@Valid @RequestBody data: CustomCommandDto): DeferredResult<ExecutionResponseDto> {
    return simModemService.executeCustomCommand(data)
  }
}