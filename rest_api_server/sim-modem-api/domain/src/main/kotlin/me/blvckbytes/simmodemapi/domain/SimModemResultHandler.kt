package me.blvckbytes.simmodemapi.domain

fun interface SimModemResultHandler {

  fun handle(result: ExecutionResult, responses: List<SimModemResponse>)

}