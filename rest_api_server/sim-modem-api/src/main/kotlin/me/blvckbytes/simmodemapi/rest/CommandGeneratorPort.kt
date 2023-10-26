package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.SimModemCommand
import me.blvckbytes.simmodemapi.modem.SimModemCommandChain
import me.blvckbytes.simmodemapi.modem.SimModemResultHandler
import me.blvckbytes.simmodemapi.modem.ValidityPeriodUnit

interface CommandGeneratorPort {

  fun forSendingSms(
    recipient: String,
    message: String,
    validityPeriodUnit: ValidityPeriodUnit?,
    validityPeriodValue: Double,
    resultHandler: SimModemResultHandler
  ): SimModemCommandChain

  fun forSignalQuality(resultHandler: SimModemResultHandler): SimModemCommandChain

  fun forSelectedCharacterSet(resultHandler: SimModemResultHandler): SimModemCommandChain

  fun forSelectableCharacterSets(resultHandler: SimModemResultHandler): SimModemCommandChain

  fun forCustomCommand(resultHandler: SimModemResultHandler, command: SimModemCommand): SimModemCommandChain

}