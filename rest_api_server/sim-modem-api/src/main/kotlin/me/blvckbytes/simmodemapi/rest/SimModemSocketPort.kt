package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.SimModemCommandChain

interface SimModemSocketPort {

  fun queueExecution(commandChain: SimModemCommandChain)

}