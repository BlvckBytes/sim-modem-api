package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.*
import me.blvckbytes.simmodemapi.domain.exception.IllegalCharacterException
import me.blvckbytes.simmodemapi.domain.exception.MessageTooLongException
import me.blvckbytes.simmodemapi.domain.pdu.*
import me.blvckbytes.simmodemapi.domain.pdu.header.ConcatenatedShortMessage
import me.blvckbytes.simmodemapi.domain.pdu.header.InformationElementIdentifier
import me.blvckbytes.simmodemapi.domain.pdu.header.UserDataHeader
import me.blvckbytes.simmodemapi.domain.port.CommandGeneratorPort
import me.blvckbytes.simmodemapi.domain.textcoder.ASCIITextCoder
import org.springframework.stereotype.Component
import java.util.*
import kotlin.math.min

@Component
class CommandGeneratorAdapter : CommandGeneratorPort {

  companion object {
    private const val DEFAULT_TIMEOUT_MS = 3000

    // TODO: This value should be configurable
    private const val MESSAGE_CENTER = "+4365009000000"

    private val CONTROL_CHARACTERS = (0..31).map { it.toChar() }
    private val PREDICATE_ENDS_IN_OK = ResponsePredicate { trimControlCharacters(it).endsWith("OK") }
    private val PREDICATE_PROMPT = ResponsePredicate { trimControlCharacters(it) == "> " }

    private fun trimControlCharacters(input: String): String {
      return input.trim { it in CONTROL_CHARACTERS }
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

  override fun forSendingSms(
    recipient: String,
    message: String,
    validityPeriodUnit: ValidityPeriodUnit?,
    validityPeriodValue: Double,
    resultHandler: SimModemResultHandler
  ): SimModemCommandChain {
    var remainingMessage = message
    var remainingMessageLength = remainingMessage.length

    val messageParts = mutableListOf<Pair<MessageEncodingResult, UserDataHeader>>()

    messageSegmentor@ while (remainingMessageLength > 0) {

      // There's only one byte available for message part numbers with
      // the currently used information element, which should be more than plenty anyways
      if (messageParts.size >= 0xFF) {
        throw MessageTooLongException(messageParts.fold(0) { accumulator, current ->
          accumulator + current.first.numberOfCharacters
        })
      }

      for (currentEncoding in PDUAlphabet.AVAILABLE_ALPHABETS_ASCENDING) {
        var currentSubstringLength = min(remainingMessageLength, currentEncoding.maximumCharacters)
        val header = UserDataHeader()

        // Message would not fit into one part, so a concatenation header is required
        if (messageParts.size > 0 || currentSubstringLength < remainingMessageLength) {
          // The number of total messages are not known yet and will be patched later
          header.addElement(ConcatenatedShortMessage(null, 1, messageParts.size + 1))
        }

        // The header takes up space of the actual message
        if (currentSubstringLength == currentEncoding.maximumCharacters)
          currentSubstringLength -= getNumberOfTakenUpCharacters(header, currentEncoding)

        val currentSubstring = remainingMessage.substring(0, currentSubstringLength)
        val encodingResult = tryEncodeMessage(currentSubstring, currentEncoding) ?: continue

        remainingMessage = remainingMessage.substring(currentSubstringLength)
        remainingMessageLength -= currentSubstringLength

        messageParts.add(Pair(encodingResult, header))
        continue@messageSegmentor
      }

      throw IllegalCharacterException()
    }

    val numberOfSegments = messageParts.size
    val commandList = mutableListOf<SimModemCommand>()

    commandList.add(makeCommand(DEFAULT_TIMEOUT_MS, PREDICATE_ENDS_IN_OK, "AT+CMGF=0\r\n"))

    for (segmentNumber in 1..numberOfSegments) {
      val (encodingResult, header) = messageParts[segmentNumber - 1]

      // Patch the total part count now that it is known
      header.getElement(InformationElementIdentifier.CONCATENATED_SHORT_MESSAGE)?.totalNumberOfParts = numberOfSegments

      makeSendSmsSegmentCommands(recipient, validityPeriodUnit, validityPeriodValue, encodingResult, header, commandList)
    }

    return SimModemCommandChain(CommandChainType.SEND_SMS, commandList, resultHandler)
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

  private fun getNumberOfTakenUpCharacters(header: UserDataHeader, alphabet: PDUAlphabet): Int {
    if (header.elements.isEmpty())
      return 0

    // Length indicator byte
    var byteLength = 1

    for (element in header.elements) {
      byteLength += when (element) {
        is ConcatenatedShortMessage -> 5
        else -> throw IllegalStateException("Unimplemented element ${element.getType().identifier}")
      }
    }

    return (byteLength * 8 + (alphabet.numberOfBits - 1)) / alphabet.numberOfBits
  }

  private fun makeCommand(
    timeoutMs: Int,
    responsePredicate: ResponsePredicate?,
    command: String
  ): SimModemCommand {
    return SimModemCommand(
      ASCIITextCoder.encode(command),
      substituteUnprintableAscii(command),
      timeoutMs,
      responsePredicate
    )
  }

  private fun tryEncodeMessage(message: String, alphabet: PDUAlphabet): MessageEncodingResult? {
    return try {
      return MessageEncodingResult(alphabet.textCoder.encode(message), message.length, alphabet)
    } catch (exception: IllegalCharacterException) {
      null
    }
  }

  private fun makeSendSmsSegmentCommands(
    recipient: String,
    validityPeriodUnit: ValidityPeriodUnit?,
    validityPeriodValue: Double,
    encodingResult: MessageEncodingResult,
    header: UserDataHeader,
    commandList: MutableList<SimModemCommand>
  ) {
    val pdu = PDU(
      PhoneNumber.fromInternationalISDN(MESSAGE_CENTER),
      MessageFlags(
        MessageType.SMS_SUBMIT,
        (
        if (validityPeriodUnit == null)
          ValidityPeriodFormat.NOT_PRESENT
        else
          ValidityPeriodFormat.RELATIVE_INTEGER
        ),
        (
          if (header.elements.isNotEmpty())
            EnumSet.of(
              BinaryMessageFlag.STATUS_REPORT_REQUEST,
              BinaryMessageFlag.HAS_USER_DATA_HEADER
            )
          else
            EnumSet.of(BinaryMessageFlag.STATUS_REPORT_REQUEST)
        )
      ),
      null,
      PhoneNumber.fromInternationalISDN(recipient),
      BinaryProtocolIdentifierFlag.FOR_SENDING_SHORT_MESSAGE,
      BinaryDCSFlag.fromAlphabetForShortMessage(encodingResult.alphabet),
      validityPeriodUnit,
      validityPeriodValue,
      header,
      ""
    )

    val writeResult = PDUWriteHelper.writePdu(pdu, encodingResult)

    commandList.add(makeCommand(DEFAULT_TIMEOUT_MS, PREDICATE_PROMPT, "AT+CMGS=${writeResult.data.size - writeResult.smscByteLength}\r\n"))
    commandList.add(makeCommand(10 * 1000, PREDICATE_ENDS_IN_OK, "${BinaryUtils.binaryToHexString(writeResult.data)}\u001A\r\n"))
  }
}