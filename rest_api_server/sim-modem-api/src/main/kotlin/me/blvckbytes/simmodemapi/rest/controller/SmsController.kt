package me.blvckbytes.simmodemapi.rest.controller

import jakarta.validation.Valid
import me.blvckbytes.simmodemapi.rest.dto.ExecutionResponseDto
import me.blvckbytes.simmodemapi.rest.SimModemService
import me.blvckbytes.simmodemapi.rest.dto.SendSmsRequestDto
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