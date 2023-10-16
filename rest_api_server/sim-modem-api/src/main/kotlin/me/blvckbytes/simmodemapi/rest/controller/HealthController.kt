package me.blvckbytes.simmodemapi.rest.health

import me.blvckbytes.simmodemapi.rest.ExecutionResponseDto
import me.blvckbytes.simmodemapi.rest.SimModemService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult

@RestController
@RequestMapping("health")
class HealthController(
  val simModemService: SimModemService
) {

  @GetMapping
  fun getHealth(): DeferredResult<ExecutionResponseDto> {
    return simModemService.getHealth()
  }
}