package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.*
import me.blvckbytes.simmodemapi.domain.exception.InvalidPduException
import me.blvckbytes.simmodemapi.domain.exception.PduInvalidityReason
import me.blvckbytes.simmodemapi.domain.header.ConcatenatedShortMessage
import me.blvckbytes.simmodemapi.domain.header.InformationElementIdentifier
import me.blvckbytes.simmodemapi.domain.header.UserDataHeader
import java.util.EnumSet

object PDUReadHelper {

  // TODO: Write test cases

  fun parsePdu(direction: PDUDirection, data: ByteArray): PDU {
    try {
      val reader = ByteArrayReader(data)
      val smsc = parseSmsc(reader)
      val messageFlags = parseMessageFlags(direction, reader)
      val messageReferenceNumber = parseMessageReferenceNumber(reader)
      val destinationAddress = parseDestination(reader)
      val protocolIdentifier = parseProtocolIdentifier(reader)
      val dcsFlags = BinaryDCSFlag.fromDCSValue(reader.readInt())
      val (header, message) = parseUserData(messageFlags, dcsFlags, reader)

      return PDU(
        smsc,
        messageFlags,
        messageReferenceNumber,
        destinationAddress,
        protocolIdentifier,
        dcsFlags,
        header,
        message
      )
    } catch (exception: EndOfByteArrayException) {
      throw InvalidPduException(PduInvalidityReason.SHORTER_THAN_EXPECTED)
    }
  }

  private fun parseUserData(
    messageFlags: MessageFlags,
    dcsFlags: EnumSet<BinaryDCSFlag>,
    reader: ByteArrayReader
  ): Pair<UserDataHeader?, String> {
    val alphabet = (
      if (dcsFlags.contains(BinaryDCSFlag.SEVEN_BIT_GSM_ALPHABET))
        PduAlphabet.GSM_SEVEN_BIT
      else if (dcsFlags.contains(BinaryDCSFlag.EIGHT_BIT_ALPHABET))
        PduAlphabet.EIGHT_BIT
      else if (dcsFlags.contains(BinaryDCSFlag.SIXTEEN_BIT_UCS2_ALPHABET))
        PduAlphabet.UCS2_SIXTEEN_BIT
      else
        throw InvalidPduException(PduInvalidityReason.INVALID_ALPHABET)
    )

    var messageLengthInUnits = reader.readInt()
    var totalHeaderLengthInBits = 0
    var header: UserDataHeader? = null

    if (messageFlags.binaryFlags.contains(BinaryMessageFlag.HAS_USER_DATA_HEADER)) {
      header = UserDataHeader()

      val headerLengthInBytes = reader.readInt()

      val preHeaderParseRemainingBytes = reader.remainingBytes

      while (preHeaderParseRemainingBytes - reader.remainingBytes < headerLengthInBytes) {
        val identifierValue = reader.readInt()
        val numberOfParameters = reader.readInt()

        when(identifierValue) {
          InformationElementIdentifier.CONCATENATED_SHORT_MESSAGE.identifier -> {

            if (numberOfParameters != 3)
              throw InvalidPduException(PduInvalidityReason.IEI_PARAMETER_COUNT_MISMATCH, "Expected 3 parameters for IEI $identifierValue")

            var messageReferenceNumber: Int? = reader.readInt()
            val totalNumberOfParts = reader.readInt()
            val sequenceNumberOfThisPart = reader.readInt()

            if (messageReferenceNumber == 0)
              messageReferenceNumber = null

            header.addElement(ConcatenatedShortMessage(messageReferenceNumber, totalNumberOfParts, sequenceNumberOfThisPart))
          }
          else -> throw InvalidPduException(PduInvalidityReason.INVALID_INFORMATION_ELEMENT_IDENTIFIER, "Unknown identifier $identifierValue")
        }
      }

      val parsedBytes = preHeaderParseRemainingBytes - reader.remainingBytes

      if (parsedBytes < headerLengthInBytes)
        throw InvalidPduException(PduInvalidityReason.HEADER_TOO_SHORT)

      if (parsedBytes > headerLengthInBytes)
        throw InvalidPduException(PduInvalidityReason.HEADER_TOO_LONG)

      totalHeaderLengthInBits = (headerLengthInBytes + 1) * 8
    }

    if (alphabet == PduAlphabet.GSM_SEVEN_BIT) {
      // 1 unit = 1 character (seven bit)
      messageLengthInUnits -= (totalHeaderLengthInBits + (7 - 1)) / 7

      val nextHeaderSeptetMultiple = (totalHeaderLengthInBits + (7 - 1)) / 7
      val leadingPaddingLengthInBits = nextHeaderSeptetMultiple * 7 - totalHeaderLengthInBits

      /*
        When packing, every 8th byte is removed, as it has been packed into the seven
        bytes before it, since each byte had one unused bit available

        chars bytes
        1-7   1-7
        8     7
        9-15  8-14
        16    14
       */

      var messageLengthInBytes = messageLengthInUnits - (messageLengthInUnits / 8)

      if (leadingPaddingLengthInBits > 0)
        messageLengthInBytes += 1

      val packedMessageBytes = reader.readBytes(messageLengthInBytes)
      val unpackedMessageBytes = unpackSevenBitCharacters(packedMessageBytes, totalHeaderLengthInBits)

      return Pair(
        header,
        GsmTextCoder.decode(unpackedMessageBytes)
          ?: throw InvalidPduException(PduInvalidityReason.INVALID_MESSAGE)
      )
    }

    if (alphabet == PduAlphabet.UCS2_SIXTEEN_BIT) {
      // 1 unit = 1 byte
      messageLengthInUnits -= totalHeaderLengthInBits / 8

      // Discard leading padding byte
      if (totalHeaderLengthInBits % 16 != 0)
        reader.readInt()

      return Pair(
        header,
        UCS2TextCoder.decode(reader.readBytes(messageLengthInUnits))
          ?: throw InvalidPduException(PduInvalidityReason.INVALID_MESSAGE)
      )
    }

    throw IllegalStateException("Unimplemented alphabet $alphabet")
  }

  private fun unpackSevenBitCharacters(bytes: ByteArray, headerLengthInBits: Int): ByteArray {
    val offset = 7 - (headerLengthInBits % 7)

    if (offset != 7) {
      var lastDroppedBits: Int? = null
      for (j in bytes.indices.reversed()) {
        val currentValue = (bytes[j].toInt() and 0xFF)
        var newValue = (currentValue shr offset)

        if (lastDroppedBits != null)
          newValue = newValue or lastDroppedBits

        bytes[j] = newValue.toByte()
        lastDroppedBits = currentValue shl (8 - offset)
      }
    }

    val result = mutableListOf<Byte>()

    var n = 1
    var i = -1

    var lastMSBs: Int? = null
    var lastMSBsN = 0

    while (++i < bytes.size) {
      val currentByte = bytes[i].toInt()

      var currentCharacter = (currentByte and BinaryUtils.nLsbMask(8 - n))

      if (lastMSBs != null)
        currentCharacter = (currentCharacter shl lastMSBsN) or (lastMSBs shr (8 - lastMSBsN))

      result.add(currentCharacter.toByte())

      lastMSBs = currentByte and BinaryUtils.nMsbMask(n)
      lastMSBsN = n

      if (++n == 8) {
        result.add((lastMSBs shr (8 - lastMSBsN)).toByte())
        lastMSBs = null
        n = 1
      }
    }

    // Discard padding artifact (see writing)
    if (result.size > 0 && result[result.size - 1] == 0x1A.toByte())
      result.removeLast()

    return result.toByteArray()
  }

  private fun parseProtocolIdentifier(reader: ByteArrayReader): Int {
    // Note that for the straightforward case of simple MS-to-SC short message transfer the Protocol
    // Identifier is set to the value 0.
    return reader.readInt()
  }

  private fun parseDestination(reader: ByteArrayReader): PhoneNumber {
    // Length (number of characters) without TON (Type Of Number) field
    var length = reader.readInt()
    val type = reader.readInt()

    // The length specified doesn't include the padding character
    if (length % 2 != 0)
      ++length

    return PhoneNumber(
      type,
      parsePhoneNumber(length / 2, PduInvalidityReason.MALFORMED_DESTINATION_NUMBER, reader)
    )
  }

  private fun parsePhoneNumber(length: Int, invalidityReason: PduInvalidityReason, reader: ByteArrayReader): String {
    val smscNumberHexString = BinaryUtils.binaryToHexString(reader.readBytes(length))
    val smscNumberBuilder = StringBuilder()

    for (i in smscNumberHexString.indices step 2) {
      val firstCharacter = smscNumberHexString[i]
      val secondCharacter = smscNumberHexString[i + 1]

      if (!secondCharacter.isDigit())
        throw InvalidPduException(invalidityReason, "Invalid digit at index ${i + 1}: $secondCharacter")

      smscNumberBuilder.append(secondCharacter)

      if (firstCharacter == 'F') {
        if (i != smscNumberHexString.length - 2)
          throw InvalidPduException(invalidityReason, "Padding character F occurred too early at index $i")

        // Ignore trailing padding character
        continue
      }

      if (!firstCharacter.isDigit())
        throw InvalidPduException(
          PduInvalidityReason.MALFORMED_SMSC_NUMBER,
          "Invalid SMSC digit at index $i: $firstCharacter"
        )

      smscNumberBuilder.append(firstCharacter)
    }

    return smscNumberBuilder.toString()
  }

  private fun parseMessageReferenceNumber(reader: ByteArrayReader): Int? {
    val number = reader.readInt()

    if (number == 0)
      return null

    return number
  }

  private fun parseMessageFlags(direction: PDUDirection, reader: ByteArrayReader): MessageFlags {
    val flags = reader.readInt()

    val messageType = MessageType.fromMessageFlagsValue(direction, flags) ?:
      throw InvalidPduException(PduInvalidityReason.INVALID_MESSAGE_TYPE)

    val validityPeriodFormat = when (messageType) {
      MessageType.SMS_SUBMIT -> (
        ValidityPeriodFormat.fromMessageFlagsValue(messageType, flags) ?:
          throw InvalidPduException(PduInvalidityReason.INVALID_VALIDITY_PERIOD_FORMAT)
      )
      else -> null
    }

    val binaryMessageFlags = BinaryMessageFlag.fromMessageFlagsValue(messageType, flags)

    return MessageFlags(messageType, validityPeriodFormat, binaryMessageFlags)
  }

  private fun parseSmsc(reader: ByteArrayReader): PhoneNumber? {
    val smscLength = reader.readInt()

    if (smscLength == 0)
      return null

    val smscTypeOfAddress = reader.readInt()
    val smscNumberLength = smscLength - 1

    return PhoneNumber(
      smscTypeOfAddress,
      parsePhoneNumber(smscNumberLength, PduInvalidityReason.MALFORMED_SMSC_NUMBER, reader)
    )
  }
}