package me.blvckbytes.simmodemapi.domain.pdu

import me.blvckbytes.simmodemapi.domain.textcoder.EightBitTextCoder
import me.blvckbytes.simmodemapi.domain.textcoder.GSMTextCoder
import me.blvckbytes.simmodemapi.domain.textcoder.TextCoder
import me.blvckbytes.simmodemapi.domain.textcoder.UCS2TextCoder

enum class PDUAlphabet(
  val textCoder: TextCoder,
  val dcsFlag: BinaryDCSFlag,
  val maximumCharacters: Int,
  val numberOfBits: Int
) {
  GSM_SEVEN_BIT(GSMTextCoder, BinaryDCSFlag.SEVEN_BIT_GSM_ALPHABET, 7),
  EIGHT_BIT(EightBitTextCoder, BinaryDCSFlag.EIGHT_BIT_ALPHABET, 8),
  UCS2_SIXTEEN_BIT(UCS2TextCoder, BinaryDCSFlag.SIXTEEN_BIT_UCS2_ALPHABET, 16),
  ;

  companion object {
    // The maximum length of the payload (called UD - User Data) dictates how many
    // characters may be carried within one short message, depending on it's encoding
    // as well as of the presence or absence of a header
    private const val MAX_UD_LENGTH = 1120

    val AVAILABLE_ALPHABETS_ASCENDING = arrayOf(
      GSM_SEVEN_BIT,
      UCS2_SIXTEEN_BIT
    )
  }

  constructor(
    textCoder: TextCoder,
    dcsFlag: BinaryDCSFlag,
    numberOfBits: Int
  ) : this(textCoder, dcsFlag, MAX_UD_LENGTH / numberOfBits, numberOfBits)
}