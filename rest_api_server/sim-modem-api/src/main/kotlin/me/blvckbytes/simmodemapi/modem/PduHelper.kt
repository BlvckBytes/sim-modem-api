package me.blvckbytes.simmodemapi.modem

import java.math.BigInteger

object PduHelper {

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
    statusReport: Boolean,
    userDataHeader: Boolean,
    replyPath: Boolean,
    output: MutableList<Byte>
  ): Int {
    /*
      One byte for six parameters, as follows:
      Bit 0,1: TP-MTI (Message Type Indication), 01 means SMS-SUBMIT
      Bit 2:   TP-RD (Reject Duplicates), 0 for off, 1 for on
      Bit 3,4: TP-VPF (Validity Period), 00 for not set
      Bit 5:   TP-SRR (Status Report Request), 0 for off, 1 for on
      Bit 6:   TP-UDHI (User Data Header Indicator), 0 for no UDH
      Bit 7:   TP-RP (Reply Path), 0 for off, 1 for on
     */
    var flags = 0b00000001

    if (rejectDuplicates)
      flags = flags or 0b00000100

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

    if (messageReferenceNumber == null) {
      output.add(0)
      return 1
    }

    output.add(messageReferenceNumber.toByte())
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
    // 0x00 could also work in most of the cases, but 0x1F is the default value, according to GSM 03.40
    output.add(0x1F)
    return 1
  }

  fun writeUserDataCodingScheme(
    isNormalSms: Boolean,
    isNotSevenBitsPerChar: Boolean,
    output: MutableList<Byte>
  ): Int {
    /*
      This byte indicates how the user data is to be parsed

      Bit 1,0: Message class, 00 means show but don't save, 01 means "normal" SMS
      Bit 2:   Encoding due to character set, 0 means GSM charset (7 bit/char), 1 means other (8 bit/char)
      Bit 3:   Reserved field, has to be 0
      Bit 7-4: Seems to dictate >this byte's< structure, and a value of 1111 represents this makeup
     */
    var scheme = 0b11110000

    if (isNormalSms)
      scheme = scheme or 0b00000001

    if (isNotSevenBitsPerChar)
      scheme = scheme or 0b00000100

    output.add(scheme.toByte())
    return 1
  }

  fun writeUserData(
    numberOfCharacters: Int,
    message: ByteArray,
    output: MutableList<Byte>
  ): Int {
    output.add(numberOfCharacters.toByte())
    message.forEach { output.add(it) }
    return message.size + 1
  }

  fun packSevenBitCharacters(septets: ByteArray): ByteArray {
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

      val sourceBits = source.toUInt() and nLsbMask(n)
      val destinationBits = destination.toUInt() and nMsbMask(n).inv()

      result[i] = (destinationBits or (sourceBits shl 8 - n)).toByte()
      result[i + 1] = (source.toUInt() shr n).toByte()

      if (++n == 8) {
        // delete source from result, as it's now 0x00
        result.removeAt(i + 1)
        n = 1
      }
    }

    return result.toByteArray()
  }

  private fun nLsbMask(n: Int): UInt {
    var mask = 0

    for (i in 0 until n)
      mask += BigInteger.valueOf(2).pow(i).toInt()

    return mask.toUInt()
  }

  private fun nMsbMask(n: Int): UInt {
    var mask = 0

    for (i in 0 until n)
      mask += BigInteger.valueOf(2).pow(8 - 1 - i).toInt()

    return mask.toUInt()
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