package me.blvckbytes.simmodemapi.rest.sms

import jakarta.validation.Valid
import me.blvckbytes.simmodemapi.rest.ExecutionResponseDto
import me.blvckbytes.simmodemapi.rest.SimModemService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult

@RestController
@RequestMapping("sms")
class SmsController(
  val simModemService: SimModemService
) {

  @PostMapping
  fun sendSms(@Valid @RequestBody data: SendSmsRequestDto): DeferredResult<ExecutionResponseDto> {
    return simModemService.sendSms(data)
  }
}