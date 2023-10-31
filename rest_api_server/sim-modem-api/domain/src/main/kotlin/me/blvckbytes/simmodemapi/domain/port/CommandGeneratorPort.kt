package me.blvckbytes.simmodemapi.domain.port

import me.blvckbytes.simmodemapi.domain.SimModemCommand
import me.blvckbytes.simmodemapi.domain.SimModemCommandChain
import me.blvckbytes.simmodemapi.domain.SimModemResultHandler
import me.blvckbytes.simmodemapi.domain.pdu.ValidityPeriodUnit

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