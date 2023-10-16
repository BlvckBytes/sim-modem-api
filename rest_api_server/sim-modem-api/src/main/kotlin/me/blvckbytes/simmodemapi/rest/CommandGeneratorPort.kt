package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.SimModemCommand
import me.blvckbytes.simmodemapi.modem.SimModemCommandChain
import me.blvckbytes.simmodemapi.modem.SimModemResultHandler

interface CommandGeneratorPort {

  fun forSendingSms(recipient: String, message: String, resultHandler: SimModemResultHandler): SimModemCommandChain

  fun forSignalQuality(resultHandler: SimModemResultHandler): SimModemCommandChain

  fun forSelectedCharacterSet(resultHandler: SimModemResultHandler): SimModemCommandChain

  fun forSelectableCharacterSets(resultHandler: SimModemResultHandler): SimModemCommandChain

  fun forCustomCommand(resultHandler: SimModemResultHandler, command: SimModemCommand): SimModemCommandChain

}