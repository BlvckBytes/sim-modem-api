package me.blvckbytes.simmodemapi.domain.port

import me.blvckbytes.simmodemapi.domain.SimModemCommandChain

interface SimModemSocketPort {

  fun queueExecution(commandChain: SimModemCommandChain)

}