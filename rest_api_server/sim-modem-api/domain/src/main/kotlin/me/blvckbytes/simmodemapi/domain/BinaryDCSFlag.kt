package me.blvckbytes.simmodemapi.domain

import java.util.*

/*
  TP-DCS field (Data Coding Scheme)

  Bits 7..4 dictate the interpretation of octet's remaining bits:

  Bits 7..4  Meaning
  00XX       General Data Coding
  1000..1011 Reserved
  1101       Message Waiting Indication Group: Discard Message
  1101       Message Waiting Indication Group: Store Message

  NOTE: It seems like everything but 00XX is not SMS-specific and can be ignored.
 */
enum class BinaryDCSFlag(
  private val value: Int,
  private val bitmask: Int
) {
  /*
    Bit 4, if set to 0, indicates that bits 1 to 0 are reserved and have no message class meaning
    Bit 4, if set to 1, indicates that bits 1 to 0 have a message class meaning:
   */

  MESSAGE_CLASS_0(0b00010000, 0b11010011),
  // Default Meaning: ME-specific
  MESSAGE_CLASS_1(0b00010001, 0b11010011),
  // SIM specific message
  MESSAGE_CLASS_2(0b00010010, 0b11010011),
  // Default Meaning: TE-specific (see GSM TS 07.05)
  MESSAGE_CLASS_3(0b00010011, 0b11010011),

  /*
    Bit 5, if set to 0, indicates the text is uncompressed
    Bit 5, if set to 1, indicates the text is compressed using the GSM standard compression algorithm
   */
  GSM_STANDARD_COMPRESSED(0b00100000, 0b11100000),

  /*
    Bits 3 and 2 indicate the alphabet being used
   */

  SEVEN_BIT_GSM_ALPHABET   (0b00000000, 0b11001100),
  EIGHT_BIT_ALPHABET       (0b00000100, 0b11001100),
  SIXTEEN_BIT_UCS2_ALPHABET(0b00001000, 0b11001100)
  // 0b00000011 Reserved
  ;

  companion object {
    fun fromDCSValue(value: Int): EnumSet<BinaryDCSFlag> {
      val result = EnumSet.noneOf(BinaryDCSFlag::class.java)

      for (type in values()) {
        if ((value and type.bitmask) == type.value)
          result.add(type)
      }

      return result
    }
  }
}