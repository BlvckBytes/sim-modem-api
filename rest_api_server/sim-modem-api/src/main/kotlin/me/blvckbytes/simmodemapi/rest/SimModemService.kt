package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.SimModemCommandChain
import me.blvckbytes.simmodemapi.modem.SimModemResultHandler
import me.blvckbytes.simmodemapi.rest.dto.SendSmsRequestDto
import me.blvckbytes.simmodemapi.rest.dto.ExecutionResponseDto
import me.blvckbytes.simmodemapi.rest.dto.SimModemCommandDto
import org.springframework.stereotype.Service
import org.springframework.web.context.request.async.DeferredResult

@Service
class SimModemService(
  val simModemSocket: SimModemSocketPort,
  val commandGenerator: CommandGeneratorPort
) {

  fun sendSms(data: SendSmsRequestDto): DeferredResult<ExecutionResponseDto> {
    return queueChain { commandGenerator.forSendingSms(data.recipient!!, data.message!!, it) }
  }

  fun getSignalQuality(): DeferredResult<ExecutionResponseDto> {
    return queueChain { commandGenerator.forSignalQuality(it) }
  }

  fun getSelectedCharacterSet(): DeferredResult<ExecutionResponseDto> {
    return queueChain { commandGenerator.forSelectedCharacterSet(it) }
  }

  fun getSelectableCharacterSets(): DeferredResult<ExecutionResponseDto> {
    return queueChain { commandGenerator.forSelectableCharacterSets(it) }
  }

  private fun queueChain(generator: (resultHandler: SimModemResultHandler) -> SimModemCommandChain): DeferredResult<ExecutionResponseDto> {
    val deferredResult = DeferredResult<ExecutionResponseDto>()
    simModemSocket.queueExecution(generator(
      SimModemResultHandler { result, responses ->
        deferredResult.setResult(ExecutionResponseDto(result, responses.map { SimModemCommandDto.fromModel(it, commandGenerator) } ))
      }
    ))
    return deferredResult
  }
}