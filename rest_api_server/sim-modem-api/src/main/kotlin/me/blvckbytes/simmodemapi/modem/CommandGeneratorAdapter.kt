package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.rest.CommandGeneratorPort
import org.springframework.stereotype.Component

@Component
class CommandGeneratorAdapter : CommandGeneratorPort {

  companion object {
    private const val DEFAULT_TIMEOUT_MS = 1000
    private const val MESSAGE_CENTER = "+4365009000000"

    private val CONTROL_CHARACTERS = (0..31).map { it.toChar() }
    private val PREDICATE_ENDS_IN_OK = ResponsePredicate { trimControlCharacters(it).endsWith("OK") }
    private val PREDICATE_PROMPT = ResponsePredicate { trimControlCharacters(it) == "> " }

    private fun trimControlCharacters(input: String): String {
      return input.trim { it in CONTROL_CHARACTERS }
    }
  }

  override fun forSendingSms(recipient: String, message: String): List<SimModemCommand> {
    return listOf(
      SimModemCommand("AT+CMGF=1\r\n", DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK),
      SimModemCommand("AT+CSCS=\"GSM\"\r\n", DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK),
      SimModemCommand("AT+CSCA=\"${MESSAGE_CENTER}\"\r\n", DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK),
      SimModemCommand("AT+CMGS=\"${recipient}\"\r\n", DEFAULT_TIMEOUT_MS, PREDICATE_PROMPT),
      SimModemCommand("${message}\u001A\r\n", 10 * 1000, PREDICATE_ENDS_IN_OK),
    )
  }

  override fun forHealth(): List<SimModemCommand> {
    return listOf(
      SimModemCommand("AT+CSQ\r\n", DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK)
    )
  }

  override fun trimControlCharacters(input: String): String {
    return Companion.trimControlCharacters(input)
  }
}