package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.BinaryUtils
import me.blvckbytes.simmodemapi.domain.pdu.PDU
import me.blvckbytes.simmodemapi.domain.pdu.PDUAlphabet
import me.blvckbytes.simmodemapi.domain.pdu.PhoneNumber
import me.blvckbytes.simmodemapi.domain.pdu.header.UserDataHeader

object PDUWriteHelper {

  private class PhoneNumberWriteResult(
    val characterLengthWithPadding: Int,
    val characterLengthWithoutPadding: Int
  )

  fun writePdu(
    pdu: PDU,
    // The segmentor needs to encode the message to figure out it's maximum length anyways, but
    // the domain model does not carry an encoded result (for obvious reasons). It would be a waste
    // to encode again, so instead of accessing the wrapper for the message, this value is accessed
    encodingResult: MessageEncodingResult,
  ): PduWriteResult {
    val pduBytes = mutableListOf<Byte>()
    val smscLength = writeSMSC(pdu.smsCenter, pduBytes)

    pduBytes.add(pdu.messageFlags.toFlag().toByte())
    pduBytes.add((pdu.messageReferenceNumber ?: 0).toByte())

    writeDestination(pdu.destination, pduBytes)

    pduBytes.add(pdu.toProtocolIdentifierFlag().toByte())
    pduBytes.add(pdu.toDCSFlag().toByte())

    if (pdu.validityPeriodUnit != null)
      pduBytes.add(pdu.toRelativeValidityPeriodValue().toByte())

    writeUserData(encodingResult, pdu.header, pduBytes)

    return PduWriteResult(pduBytes.toByteArray(), smscLength)
  }

  private fun writeSMSC(smsCenter: PhoneNumber?, output: MutableList<Byte>): Int {
    // SMSC means Short Message Service Centre

    if (smsCenter == null) {
      // Length zero instructs to use the default value
      output.add(0)
      return 1
    }

    // Size byte
    var length = 1
    val lengthIndex = output.size

    output.add(smsCenter.toTypeFlag().toByte())
    ++length

    val phoneNumberWriteResult = writePhoneNumber(smsCenter.number, output)
    val phoneNumberByteLength = phoneNumberWriteResult.characterLengthWithPadding / 2

    output.add(lengthIndex, (phoneNumberByteLength + 1).toByte())
    length += phoneNumberByteLength

    return length
  }

  private fun writeDestination(phoneNumber: PhoneNumber, output: MutableList<Byte>): Int {
    val lengthIndex = output.size

    output.add(phoneNumber.toTypeFlag().toByte())

    val phoneNumberWriteResult = writePhoneNumber(phoneNumber.number, output)
    output.add(lengthIndex, phoneNumberWriteResult.characterLengthWithoutPadding.toByte())

    // TOA byte counts into total length
    return 1 + phoneNumberWriteResult.characterLengthWithPadding / 2
  }

  private fun writePhoneNumber(phoneNumber: String, output: MutableList<Byte>): PhoneNumberWriteResult {
    /*
      Phone number, as BCD (Binary Coded Decimal), in alternating order
      An uneven length is padded with a trailing F
     */
    var paddedPhoneNumber = phoneNumber

    // Prefix or escape digits shall not be included
    if (paddedPhoneNumber.startsWith("+"))
      paddedPhoneNumber = paddedPhoneNumber.substring(1)

    val needsPadding = paddedPhoneNumber.length % 2 != 0

    /*
      If the Address contains an odd number of digits, bits 5 to 8 of the last
      octet shall be filled with an end mark coded as "1111".
     */
    if (needsPadding)
      paddedPhoneNumber += "F"

    paddedPhoneNumber.chunked(2).forEach {
      output.add(
        it
          /*
            The number digit(s) in octet 1 precedes the digit(s) in octet 2 etc.
            The number digit which would be entered first is located in octet 1, bits 1 to 4.

            => Octets are in entering order, but each octet contains two digits, and
               those digits are swapped.
           */
          .reversed()

          /*
            Each digit is encoded to a BCD-nibble, as follows:
            0000 0
            0001 1
            0010 2
            0011 3
            0100 4
            0101 5
            0110 6
            0111 7
            1000 8
            1001 9
            1010 *
            1011 #
            1100 a
            1101 b
            1110 c
            1111 Used as an end-mark in the case of an odd number of digits

            NOTE: For now, *,#,a,b,c are unsupported.
           */
          .toInt(16)
          .toByte()
      )
    }

    return PhoneNumberWriteResult(
      paddedPhoneNumber.length,
      if (needsPadding) paddedPhoneNumber.length - 1 else paddedPhoneNumber.length
    )
  }

  private fun writeUserDataHeader(
    header: UserDataHeader,
    output: MutableList<Byte>
  ): Int {
    val elements = header.elements

    if (elements.isEmpty())
      return 0

    val previousLength = output.size

    for (element in elements)
      element.write(output)

    output.add(previousLength, (output.size - previousLength).toByte())

    return output.size - previousLength
  }

  private fun writeUserData(
    encodingResult: MessageEncodingResult,
    header: UserDataHeader?,
    output: MutableList<Byte>,
  ): Int {
    val previousLength = output.size

    /*
      The TP-User-Data field contains up to 140 octets of user data

      For the GSM 7-bit alphabet, it's the number of characters (septets) plus the
      number of septets in the UDH, including padding

      For the 8-bit alphabet, it's the number of characters (octets) plus the
      number of octets in the UDH (no need for padding)

      For the UCS2 16-bit alphabet, it's the number of octets (2*characters) plus
      the number of octets in the UDH (no need for padding)

      For compressed GSM 7-bit or UCS2 16-bit alphabet, it's the number of octets in
      the compressed message plus the number of uncompressed octets in the UDH, including any padding

      If this field is zero, there's no user data present
     */

    val udhLength = (
      if (header == null)
        0
      else
        writeUserDataHeader(header, output)
    )

    var messageBytes = encodingResult.bytes

    if (encodingResult.alphabet == PDUAlphabet.GSM_SEVEN_BIT)
      messageBytes = packSevenBitCharacters(encodingResult.bytes, udhLength)

    else if (
      encodingResult.alphabet == PDUAlphabet.UCS2_SIXTEEN_BIT &&
      udhLength != 0 && udhLength % 2 != 0
    ) {
      // The header is not a multiple of two bytes long, add one byte of padding to the message
      val list = messageBytes.toMutableList()
      list.add(0, 0x00)
      messageBytes = list.toByteArray()
    }

    output.add(previousLength, when (encodingResult.alphabet) {
      PDUAlphabet.GSM_SEVEN_BIT -> {
        val numberOfHeaderSeptets = if (udhLength == 0) 0 else (udhLength * 8 + (7 - 1)) / 7
        val numberOfMessageSeptets = encodingResult.numberOfCharacters
        numberOfHeaderSeptets + numberOfMessageSeptets
      }

      PDUAlphabet.EIGHT_BIT -> udhLength + encodingResult.numberOfCharacters
      PDUAlphabet.UCS2_SIXTEEN_BIT -> udhLength + encodingResult.bytes.size
      else -> throw IllegalStateException("Unsupported alphabet encountered: ${encodingResult.alphabet}")
    }.toByte())

    messageBytes.forEach { output.add(it) }

    return output.size - previousLength
  }

  private fun packSevenBitCharacters(septets: ByteArray, headerLength: Int): ByteArray {
    /*
      Since the MSb of octet n is always unused, it becomes the LSb of octet n + 1
      Now, the first two MSbs of octet n + 1 are unused, and are filled with the two LSbs of octet n + 2
      Now, the first three MSbs of octet n + 2 are unused, and are filled with the three LSbs of octet n + 3
      And so on and so forth...
     */

    val result = septets.toMutableList()

    var n = 1
    var i = -1

    while (++i < result.size - 1) {
      val destination = result[i]
      val source = result[i + 1]

      val sourceBits = source.toInt() and BinaryUtils.nLsbMask(n)
      val destinationBits = destination.toInt() and BinaryUtils.nMsbMask(n).inv()

      result[i] = (destinationBits or (sourceBits shl 8 - n)).toByte()
      result[i + 1] = ((source.toInt() and 0xFF) shr n).toByte()

      if (++n == 8) {
        result.removeAt(i + 1)
        n = 1
      }
    }

    /*
      To be honest, I still don't fully understand why this kind of manipulation
      causes the actual message within the UD, after the UDH to start at a septet boundary.

      The specification says that this is being done for backwards compatibility, so that older
      devices would just print the UDH as garbage, but the message is at least readable. The UDH
      has to be preserved as is and must not be scrambled by this septet "packing" algorithm. Somehow,
      the following manipulation also ensures that the `n` from above is also offset properly,
      because old devices would start unpacking the whole thing at once, with the pattern starting
      from the very beginning, repeating itself, so these bytes need to account for that.

      I guess that this will become more clear as soon as I get to write a PDU decoder and some tests.
     */

    val offset = 7 - ((headerLength * 8) % 7)

    if (offset != 7) {
      var lastDroppedBits: Int? = null
      for (j in result.indices) {
        val currentValue = (result[j].toInt() and 0xFF)
        var newValue = (currentValue shl offset)

        if (lastDroppedBits != null)
          newValue = newValue or lastDroppedBits

        result[j] = newValue.toByte()
        lastDroppedBits = currentValue shr (8 - offset)
      }

      if (lastDroppedBits != null) {
        // Avoid trailing '@' by having a single binary 0 that was dropped
        // and now causes another zero byte. 1A is \r << 1
        if (lastDroppedBits == 0 && offset == 1)
          result.add(0x1A)
        else
          result.add(lastDroppedBits.toByte())
      }
    }

    return result.toByteArray()
  }
}