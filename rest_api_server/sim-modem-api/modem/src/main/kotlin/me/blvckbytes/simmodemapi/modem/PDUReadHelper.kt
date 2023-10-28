package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.*
import me.blvckbytes.simmodemapi.domain.exception.InvalidPduException
import me.blvckbytes.simmodemapi.domain.exception.PduInvalidityReason

object PDUReadHelper {

  fun parsePdu(direction: PDUDirection, data: ByteArray): PDU {
    try {
      val reader = ByteArrayReader(data)
      val smsc = parseSmsc(reader)
      val messageFlags = parseMessageFlags(direction, reader)
      val messageReferenceNumber = parseMessageReferenceNumber(reader)

      return PDU(smsc, messageFlags, messageReferenceNumber)
    } catch (exception: EndOfByteArrayException) {
      throw InvalidPduException(PduInvalidityReason.SHORTER_THAN_EXPECTED)
    }
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

    if (smscNumberLength % 2 != 0)
      throw InvalidPduException(PduInvalidityReason.MALFORMED_SMSC_NUMBER, "The SMSC length has to be even")

    val smscNumberHexString = BinaryUtils.binaryToHexString(reader.readBytes(smscNumberLength))
    val smscNumberBuilder = StringBuilder()

    for (i in smscNumberHexString.indices step 2) {
      val firstCharacter = smscNumberHexString[i]
      val secondCharacter = smscNumberHexString[i + 1]

      if (!secondCharacter.isDigit())
        throw InvalidPduException(PduInvalidityReason.MALFORMED_SMSC_NUMBER, "Invalid SMSC digit at index ${i + 1}: $secondCharacter")

      smscNumberBuilder.append(secondCharacter)

      if (firstCharacter == 'F') {
        if (i != smscNumberHexString.length - 2)
          throw InvalidPduException(PduInvalidityReason.MALFORMED_SMSC_NUMBER, "SMSC padding character F occurred too early at index $i")

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

    return PhoneNumber(smscTypeOfAddress, smscNumberBuilder.toString())
  }
}

//fun main() {
//  val pduString = "079194712272303325000C9194711232547600000BD4F29C4E2FE3E9BA4D19"
//  val pdu = PDUReadHelper.parsePdu(PDUDirection.MS_TO_SC, pduString.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
//  println("smsCenter.typeOfAddress=${pdu.smsCenter?.typeOfAddress}")
//  println("smsCenter.phoneNumber=${pdu.smsCenter?.phoneNumber}")
//  println("messageFlags.messageType=${pdu.messageFlags.messageType}")
//  println("messageFlags.validityPeriodFormat=${pdu.messageFlags.validityPeriodFormat}")
//  println("messageFlags.binaryFlags=${pdu.messageFlags.binaryFlags.joinToString()}")
//  println("messageReferenceNumber=${pdu.messageReferenceNumber}")
//}