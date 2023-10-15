package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.SimModemCommand
import me.blvckbytes.simmodemapi.modem.SimModemResultHandler

interface SimModemSocketPort {

  fun queueExecution(commandChain: List<SimModemCommand>, resultHandler: SimModemResultHandler)

}