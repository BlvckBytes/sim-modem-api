package me.blvckbytes.simmodemapi.rest.controller

import me.blvckbytes.simmodemapi.rest.dto.ExecutionResponseDto
import me.blvckbytes.simmodemapi.rest.service.SimModemService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult

@RestController
@RequestMapping("info")
class InfoController(
  val simModemService: SimModemService
) {

  @GetMapping("/signal-quality")
  fun getSignalQuality(): DeferredResult<ExecutionResponseDto> {
    return simModemService.getSignalQuality()
  }

  @GetMapping("/selected-character-set")
  fun getCharacterSet(): DeferredResult<ExecutionResponseDto> {
    return simModemService.getSelectedCharacterSet()
  }

  @GetMapping("/selectable-character-sets")
  fun getCharacterSets(): DeferredResult<ExecutionResponseDto> {
    return simModemService.getSelectableCharacterSets()
  }
}