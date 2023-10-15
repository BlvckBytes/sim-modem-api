package me.blvckbytes.simmodemapi.rest

import me.blvckbytes.simmodemapi.modem.SimModemCommand

interface CommandGeneratorPort {

  fun forSendingSms(recipient: String, message: String): List<SimModemCommand>

  fun forHealth(): List<SimModemCommand>

  fun trimControlCharacters(input: String): String

}