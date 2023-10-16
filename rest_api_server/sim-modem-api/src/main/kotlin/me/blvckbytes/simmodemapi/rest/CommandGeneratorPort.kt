package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.SimModemCommandChain
import me.blvckbytes.simmodemapi.modem.SimModemResultHandler

interface CommandGeneratorPort {

  fun forSendingSms(recipient: String, message: String, resultHandler: SimModemResultHandler): SimModemCommandChain

  fun forHealth(resultHandler: SimModemResultHandler): SimModemCommandChain

  fun trimControlCharacters(input: String): String

}