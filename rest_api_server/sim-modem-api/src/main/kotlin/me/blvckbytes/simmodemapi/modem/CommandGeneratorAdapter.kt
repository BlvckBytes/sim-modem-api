package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.rest.CommandGeneratorPort
import org.springframework.stereotype.Component

@Component
class CommandGeneratorAdapter : CommandGeneratorPort {

  companion object {
    private const val DEFAULT_TIMEOUT_MS = 1000

    // TODO: This value should be configurable
    private const val MESSAGE_CENTER = "+4365009000000"

    private val CONTROL_CHARACTERS = (0..31).map { it.toChar() }
    private val PREDICATE_ENDS_IN_OK = ResponsePredicate { trimControlCharacters(it).endsWith("OK") }
    private val PREDICATE_PROMPT = ResponsePredicate { trimControlCharacters(it) == "> " }

    private fun trimControlCharacters(input: String): String {
      return input.trim { it in CONTROL_CHARACTERS }
    }

    fun binaryToHexString(buffer: ByteArray): String {
      val result = StringBuilder()
      for (byte in buffer)
        result.append("%02X".format(byte))
      return result.toString()
    }

    fun substituteUnprintableAscii(value: String): String {
      val valueCharacters = value.toCharArray()
      val result = StringBuilder()

      for (char in valueCharacters) {
        if (char.code >= 32) {
          result.append(char)
          continue
        }

        result.append('\\')

        when (char) {
          '\n' -> result.append('n')
          '\r' -> result.append('r')
          '\t' -> result.append('t')
          else -> result.append("x%02X".format(char.code))
        }
      }

      return result.toString()
    }
  }

  private class MessageEncodingResult(
    val bytes: ByteArray,
    val alphabet: PduAlphabet
  )

  override fun forSendingSms(recipient: String, message: String, resultHandler: SimModemResultHandler): SimModemCommandChain {
    // TODO: Segmentation for long messages
    val encodingResult = tryEncodeMessage(message)

    val pduBytes = mutableListOf<Byte>()
    val smscLength = PduHelper.writeSMSC(MESSAGE_CENTER, pduBytes)

    PduHelper.writeMessageFlags(
      rejectDuplicates = false,
      statusReport = true,
      userDataHeader = false,
      replyPath = false,
      pduBytes
    )

    PduHelper.writeMessageReferenceNumber(null, pduBytes)
    PduHelper.writeDestination(recipient, pduBytes)
    PduHelper.writeProtocolIdentifier(pduBytes)
    PduHelper.writeDataCodingScheme(encodingResult.alphabet, pduBytes)
    PduHelper.writeUserData(encodingResult.bytes, pduBytes)

    return SimModemCommandChain(CommandChainType.SEND_SMS, listOf(
      makeCommand(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, "AT+CMGF=0\r\n"),
      makeCommand(DEFAULT_TIMEOUT_MS, PREDICATE_PROMPT, "AT+CMGS=${pduBytes.size - smscLength}\r\n"),
      makeCommand(10 * 1000, PREDICATE_ENDS_IN_OK, "${binaryToHexString(pduBytes.toByteArray())}\u001A\r\n")
    ), resultHandler)
  }

  override fun forSignalQuality(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(CommandChainType.SIGNAL_QUALITY, listOf(
      makeCommand(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, "AT+CSQ\r\n")
    ), resultHandler)
  }

  override fun forSelectedCharacterSet(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(CommandChainType.SELECTED_CHARACTER_SET, listOf(
      makeCommand(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, "AT+CSCS?\r\n")
    ), resultHandler)
  }

  override fun forSelectableCharacterSets(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(CommandChainType.SELECTABLE_CHARACTER_SETS, listOf(
      makeCommand(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, "AT+CSCS=?\r\n")
    ), resultHandler)
  }

  override fun forCustomCommand(resultHandler: SimModemResultHandler, command: SimModemCommand): SimModemCommandChain {
    return SimModemCommandChain(CommandChainType.CUSTOM_COMMAND, listOf(command), resultHandler)
  }

  private fun makeCommand(
    timeoutMs: Int,
    responsePredicate: ResponsePredicate?,
    command: String
  ): SimModemCommand {
    return SimModemCommand(
      AsciiTextCoder.encode(command),
      substituteUnprintableAscii(command),
      timeoutMs,
      responsePredicate
    )
  }

  private fun tryEncodeMessage(message: String): MessageEncodingResult {
    return try {
      val messageBytes = GsmTextCoder.encode(message)
      MessageEncodingResult(PduHelper.packSevenBitCharacters(messageBytes), PduAlphabet.GSM_SEVEN_BIT)
    } catch (exception: IllegalCharacterException) {
      MessageEncodingResult(UCS2TextCoder.encode(message), PduAlphabet.UCS2_SIXTEEN_BIT)
    }
  }
}