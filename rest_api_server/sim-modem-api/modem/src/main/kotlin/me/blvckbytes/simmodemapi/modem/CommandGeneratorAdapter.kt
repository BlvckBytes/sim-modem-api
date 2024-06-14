package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.*
import me.blvckbytes.simmodemapi.domain.exception.IllegalCharacterException
import me.blvckbytes.simmodemapi.domain.exception.MessageTooLongException
import me.blvckbytes.simmodemapi.domain.pdu.*
import me.blvckbytes.simmodemapi.domain.pdu.header.ConcatenatedShortMessage
import me.blvckbytes.simmodemapi.domain.pdu.header.UserDataHeader
import me.blvckbytes.simmodemapi.domain.port.CommandGeneratorPort
import me.blvckbytes.simmodemapi.domain.textcoder.ASCIITextCoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import kotlin.math.min

@Component
class CommandGeneratorAdapter(
  @Value("\${modem.message-center}")
  val modemMessageCenter: String
) : CommandGeneratorPort {

  companion object {
    private val PREDICATE_ENDS_IN_OK = ResponsePredicate { ASCIITextCoder.trimControlCharacters(it).endsWith("OK") }
    private val PREDICATE_PROMPT = ResponsePredicate { ASCIITextCoder.trimControlCharacters(it) == "> " }
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
          header.add(ConcatenatedShortMessage(null, 1, messageParts.size + 1))
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

    commandList.add(makeCommand(SimModemCommandType.SET_TEXT_MODE, PREDICATE_ENDS_IN_OK, "AT+CMGF=0\r\n"))

    for (segmentNumber in 1..numberOfSegments) {
      val (encodingResult, header) = messageParts[segmentNumber - 1]

      // Patch the total part count now that it is known
      header.getConcatenatedShortMessage()?.totalNumberOfParts = numberOfSegments

      makeSendSmsSegmentCommands(recipient, validityPeriodUnit, validityPeriodValue, encodingResult, header, commandList)
    }

    return SimModemCommandChain(commandList, resultHandler)
  }

  override fun forSignalQuality(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(listOf(
      makeCommand(SimModemCommandType.SIGNAL_QUALITY, PREDICATE_ENDS_IN_OK, "AT+CSQ\r\n")
    ), resultHandler)
  }

  override fun forSubscriberNumber(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(listOf(
      makeCommand(SimModemCommandType.SUBSCRIBER_NUMBER, PREDICATE_ENDS_IN_OK, "AT+CNUM\r\n")
    ), resultHandler)
  }

  override fun forSelectedCharacterSet(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(listOf(
      makeCommand(SimModemCommandType.SELECTED_CHARACTER_SET, PREDICATE_ENDS_IN_OK, "AT+CSCS?\r\n")
    ), resultHandler)
  }

  override fun forSelectableCharacterSets(resultHandler: SimModemResultHandler): SimModemCommandChain {
    return SimModemCommandChain(listOf(
      makeCommand(SimModemCommandType.SELECTABLE_CHARACTER_SETS, PREDICATE_ENDS_IN_OK, "AT+CSCS=?\r\n")
    ), resultHandler)
  }

  override fun forCustomCommand(resultHandler: SimModemResultHandler, command: SimModemCommand): SimModemCommandChain {
    return SimModemCommandChain(listOf(command), resultHandler)
  }

  private fun getNumberOfTakenUpCharacters(header: UserDataHeader, alphabet: PDUAlphabet): Int {
    if (header.isEmpty())
      return 0

    // Start with one, because of the length indicator byte
    val byteLength = header.fold(1) { accumulator, current -> accumulator + current.getLengthInBytes() }

    return (byteLength * 8 + (alphabet.numberOfBits - 1)) / alphabet.numberOfBits
  }

  private fun makeCommand(
    type: SimModemCommandType,
    responsePredicate: ResponsePredicate?,
    command: String
  ): SimModemCommand {
    return SimModemCommand(
      type,
      ASCIITextCoder.encode(command),
      ASCIITextCoder.substituteUnprintableAscii(command),
      null,
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
      PhoneNumber.fromInternationalISDN(modemMessageCenter),
      MessageFlags(
        MessageType.SMS_SUBMIT,
        (
        if (validityPeriodUnit == null)
          ValidityPeriodFormat.NOT_PRESENT
        else
          ValidityPeriodFormat.RELATIVE_INTEGER
        ),
        (
          if (header.isNotEmpty())
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

    commandList.add(makeCommand(SimModemCommandType.SEND_SMS_PROMPT, PREDICATE_PROMPT, "AT+CMGS=${writeResult.data.size - writeResult.smscByteLength}\r\n"))
    commandList.add(makeCommand(SimModemCommandType.SEND_SMS_BODY, PREDICATE_ENDS_IN_OK, "${BinaryUtils.binaryToHexString(writeResult.data)}\u001A\r\n"))
  }
}