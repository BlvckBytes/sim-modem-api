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

  override fun forSendingSms(recipient: String, message: String, resultHandler: SimModemResultHandler): SimModemCommandChain {
    val gsmCodedMessage = GsmTextCoder.encode(message)
    val numberOfCharacters = gsmCodedMessage.size
    val packedMessage = PduHelper.packSevenBitCharacters(gsmCodedMessage)

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
    PduHelper.writeUserDataCodingScheme(isNormalSms = true, isNotSevenBitsPerChar = false, pduBytes)
    PduHelper.writeUserData(numberOfCharacters, packedMessage, pduBytes)

    return SimModemCommandChain(CommandChainType.SEND_SMS, listOf(
      // Set SMS input mode to PDU
      makeCommandFromParts(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, Pair("AT+CMGF=0\r\n", AsciiTextCoder)),
      makeCommandFromParts(
        DEFAULT_TIMEOUT_MS, PREDICATE_PROMPT,
        Pair("AT+CMGS=${pduBytes.size - smscLength}\r\n", AsciiTextCoder),
        Pair("\r\n", AsciiTextCoder),
      ),
      makeCommandFromParts(
        10 * 1000, PREDICATE_ENDS_IN_OK,
        Pair(binaryToHexString(pduBytes.toByteArray()), AsciiTextCoder),
        Pair("\u001A", AsciiTextCoder),
      )
    ), resultHandler)
  }

  override fun forSignalQuality(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(CommandChainType.SIGNAL_QUALITY, listOf(
      makeCommandFromParts(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, Pair("AT+CSQ\r\n", AsciiTextCoder))
    ), resultHandler)
  }

  override fun forSelectedCharacterSet(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(CommandChainType.SELECTED_CHARACTER_SET, listOf(
      makeCommandFromParts(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, Pair("AT+CSCS?\r\n", AsciiTextCoder))
    ), resultHandler)
  }

  override fun forSelectableCharacterSets(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(CommandChainType.SELECTABLE_CHARACTER_SETS, listOf(
      makeCommandFromParts(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, Pair("AT+CSCS=?\r\n", AsciiTextCoder))
    ), resultHandler)
  }

  override fun forCustomCommand(resultHandler: SimModemResultHandler, command: SimModemCommand): SimModemCommandChain {
    return SimModemCommandChain(CommandChainType.CUSTOM_COMMAND, listOf(command), resultHandler)
  }

  private fun makeCommandFromParts(
    timeoutMs: Int,
    responsePredicate: ResponsePredicate?,
    vararg commandParts: Pair<String, TextCoder>
  ): SimModemCommand {
    val readableResult = StringBuilder()
    val commandArrays = mutableListOf<ByteArray>()
    var totalByteLength = 0

    for (commandPart in commandParts) {
      val encodedPart = commandPart.second.encode(commandPart.first)
      commandArrays.add(encodedPart)
      totalByteLength += encodedPart.size
      readableResult.append(substituteUnprintableAscii(commandPart.first))
    }

    val resultArray = ByteArray(totalByteLength)
    var resultIndex = 0

    for (commandArray in commandArrays) {
      commandArray.copyInto(resultArray, resultIndex)
      resultIndex += commandArray.size
    }

    return SimModemCommand(
      resultArray,
      readableResult.toString(),
      timeoutMs,
      responsePredicate
    )
  }
}