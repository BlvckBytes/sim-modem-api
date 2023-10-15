package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.SimModemCommand
import me.blvckbytes.simmodemapi.rest.sms.SendSmsRequestDto
import org.springframework.stereotype.Service
import org.springframework.web.context.request.async.DeferredResult

@Service
class SimModemService(
  val simModemSocket: SimModemSocketPort,
  val commandGenerator: CommandGeneratorPort
) {

  fun sendSms(data: SendSmsRequestDto): DeferredResult<ExecutionResponseDto> {
    return queueChain(commandGenerator.forSendingSms(data.recipient!!, data.message!!))
  }

  fun getHealth(): DeferredResult<ExecutionResponseDto> {
    return queueChain(commandGenerator.forHealth())
  }

  private fun queueChain(commandChain: List<SimModemCommand>): DeferredResult<ExecutionResponseDto> {
    val deferredResult = DeferredResult<ExecutionResponseDto>()

    simModemSocket.queueExecution(commandChain) { result, responses ->
      deferredResult.setResult(ExecutionResponseDto(result, responses.map {
        SimModemCommandDto.fromModel(it, commandGenerator)
      }))
    }

    return deferredResult
  }
}