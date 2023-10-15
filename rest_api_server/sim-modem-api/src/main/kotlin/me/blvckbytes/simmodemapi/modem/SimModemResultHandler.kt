package me.blvckbytes.simmodemapi.modem

fun interface SimModemResultHandler {

  fun handle(result: ExecutionResult, responses: List<SimModemResponse>)

}