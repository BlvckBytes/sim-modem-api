package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.BinaryUtils
import me.blvckbytes.simmodemapi.domain.PduAlphabet
import me.blvckbytes.simmodemapi.domain.ValidityPeriodUnit
import me.blvckbytes.simmodemapi.domain.header.UserDataHeader

object PDUWriteHelper {

  private class PhoneNumberWriteResult(
    val characterLengthWithPadding: Int,
    val characterLengthWithoutPadding: Int
  )

  fun writeSMSC(phoneNumber: String?, output: MutableList<Byte>): Int {
    // SMSC means Short Message Service Centre

    if (phoneNumber == null) {
      // Length zero instructs to use the default value
      output.add(0)
      return 1
    }

    var length = 1
    val lengthIndex = output.size

    length += writeTOA(output)

    val phoneNumberWriteResult = writePhoneNumber(phoneNumber, output)
    val phoneNumberByteLength = phoneNumberWriteResult.characterLengthWithPadding / 2

    output.add(lengthIndex, (phoneNumberByteLength + 1).toByte())
    length += phoneNumberByteLength

    return length
  }

  fun writeMessageFlags(
    rejectDuplicates: Boolean,
    validityPeriod: Boolean,
    statusReport: Boolean,
    userDataHeader: Boolean,
    replyPath: Boolean,
    output: MutableList<Byte>
  ): Int {
    // MS - Mobile Station, SC - SMS Center

    /*
      One byte for six parameters, as follows:
      Bit 0,1: TP-MTI (Message Type Indication)
      00 SMS-DELIVER SC->MS
      00 SMS-DELIVER REPORT MS->SC
      10 SMS-STATUS-REPORT SC->MS
      10 SMS-COMMAND MS->SC
      01 SMS-SUBMIT MS->SC
      01 SMS-SUBMIT REPORT SC->MS
      11 RESERVED
      Bit 2:   TP-RD (Reject Duplicates), 0 for off, 1 for on
      Bit 3,4: TP-VPF (Validity Period):
      00 not set
      10 present and integer represent (relative)
      01 reserved
      11 present and semi-octet represented (absolute)
      Bit 5:   TP-SRR (Status Report Request), 0 for off, 1 for on
      Bit 6:   TP-UDHI (User Data Header Indicator)
      0 The TP-UD field contains only the short message
      1 The beginning of the TP-UD field contains a header in addition to the short message
      Bit 7:   TP-RP (Reply Path), 0 for off, 1 for on
     */
    var flags = 0b00000001

    if (rejectDuplicates)
      flags = flags or 0b00000100

    // Relative validity period (since received by SC), comprised of a single byte
    if (validityPeriod)
      flags = flags or 0b00010000

    if (statusReport)
      flags = flags or 0b00100000

    if (userDataHeader)
      flags = flags or 0b01000000

    if (replyPath)
      flags = flags or 0b10000000

    output.add(flags.toByte())
    return 1
  }

  fun writeMessageReferenceNumber(messageReferenceNumber: Int?, output: MutableList<Byte>): Int {
    if (messageReferenceNumber == 0)
      throw IllegalStateException("Value zero is reserved, please use NULL for auto-generation")

    output.add((messageReferenceNumber ?: 0).toByte())
    return 1
  }

  fun writeDestination(phoneNumber: String, output: MutableList<Byte>): Int {
    val lengthIndex = output.size
    val toaLength = writeTOA(output)

    val phoneNumberWriteResult = writePhoneNumber(phoneNumber, output)
    output.add(lengthIndex, phoneNumberWriteResult.characterLengthWithoutPadding.toByte())

    return toaLength + phoneNumberWriteResult.characterLengthWithPadding / 2
  }

  fun writeProtocolIdentifier(output: MutableList<Byte>): Int {
    output.add(0x00)
    return 1
  }

  fun writeDataCodingScheme(
    alphabet: PduAlphabet,
    output: MutableList<Byte>
  ): Int {
    /*
      This byte indicates how the user data is to be parsed

      Bit 1,0: Message class, 00 means show but don't save, 01 means "normal" SMS
      Bit 2:   Encoding due to character set, 0 means GSM charset (7 bit/char), 1 means other (8 bit/char)
      Bit 3:   Reserved field, has to be 0
      Bit 7-4: Coding Group, 00XX - General data coding, then:
      Bit 5:   1 means GSM standard compressed, 0 means uncompressed
      Bit 4:   1 means bits 1,0 store a message class, 0 means that they have no meaning
      Bit 3,2: Indicate used alphabet, as follows:
      00 Default Alphabet (GSM 7 bit)
      01 8 bit
      10 UCS2 (16 bit)
      11 Reserved
     */
    output.add((0b00010001 or alphabet.bitPattern).toByte())
    return 1
  }

  fun writeValidityPeriod(
    unit: ValidityPeriodUnit,
    value: Double,
    output: MutableList<Byte>
  ): Int {
    /*
      This byte indicates for how long this message is still valid to deliver to the
      recipient, relative to when the SC received it.

      TP-VP value  Validity period value
      0 to 143     (TP-VP + 1) x 5 minutes (i.e. 5 minutes intervals up to 12 hours)
      144 to 167   12 hours + ((TP-VP -143) x 30 minutes)
      168 to 196   (TP-VP - 166) x 1 day
      197 to 255   (TP-VP - 192) x 1 week

      =>

      0-143: 5, 10, 15, ... 715, 720 (12h)
      144-167: 12.5h, 13h, 13.5h, ... 23.5h, 24h
      168-196: 2d, 3d, 4d, ... 29d, 30d
      197-255: 5w, 6w, 7w, ..., 62w, 63w
     */

    output.add(when (unit) {
      ValidityPeriodUnit.MINUTES -> (value / 5) - 1
      ValidityPeriodUnit.HOURS -> (value - 12) * 2 + 143
      ValidityPeriodUnit.DAYS -> (value + 166).toInt()
      ValidityPeriodUnit.WEEKS -> (value + 192).toInt()
    }.toByte())

    return 1
  }

  fun writeUserData(
    encodingResult: MessageEncodingResult,
    header: UserDataHeader,
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

    val udhLength = header.write(output)
    var messageBytes = encodingResult.bytes

    if (encodingResult.alphabet == PduAlphabet.GSM_SEVEN_BIT)
      messageBytes = packSevenBitCharacters(encodingResult.bytes, udhLength)

    else if (
      encodingResult.alphabet == PduAlphabet.UCS2_SIXTEEN_BIT &&
      udhLength != 0 && udhLength % 2 != 0
    ) {
      // The header is not a multiple of two bytes long, add one byte of padding to the message
      val list = messageBytes.toMutableList()
      list.add(0, 0x00)
      messageBytes = list.toByteArray()
    }

    output.add(previousLength, when (encodingResult.alphabet) {
      PduAlphabet.GSM_SEVEN_BIT -> {
        val numberOfHeaderSeptets = if (udhLength == 0) 0 else (udhLength * 8 + (7 - 1)) / 7
        val numberOfMessageSeptets = encodingResult.numberOfCharacters
        numberOfHeaderSeptets + numberOfMessageSeptets
      }

      PduAlphabet.EIGHT_BIT -> udhLength + encodingResult.numberOfCharacters
      PduAlphabet.UCS2_SIXTEEN_BIT -> udhLength + encodingResult.bytes.size
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

  private fun writeTOA(output: MutableList<Byte>): Int {
  /*
    TOA (Type Of Address)
    Bit 7:       always 1
    Bit 6,5,4:   TON (Type Of Number), 001 means international number, 000 means "unknown"
    Bit 3,2,1,0: NPI (Numbering Plan Identifier), 0001 means E.164/E.163
   */
    output.add(0b1_001_0001.toByte())
    return 1
  }

  private fun writePhoneNumber(phoneNumber: String, output: MutableList<Byte>): PhoneNumberWriteResult {
    /*
      Phone number, as BCD (Binary Coded Decimal), in alternating order
      An uneven length is padded with a trailing F
     */
    var paddedPhoneNumber = phoneNumber

    if (paddedPhoneNumber.startsWith("+"))
      paddedPhoneNumber = paddedPhoneNumber.substring(1)

    val needsPadding = paddedPhoneNumber.length % 2 != 0

    if (needsPadding)
      paddedPhoneNumber += "F"

    paddedPhoneNumber.chunked(2).forEach {
      output.add(it.reversed().toInt(16).toByte())
    }

    return PhoneNumberWriteResult(
      paddedPhoneNumber.length,
      if (needsPadding) paddedPhoneNumber.length - 1 else paddedPhoneNumber.length
    )
  }
}