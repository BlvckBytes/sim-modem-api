package me.blvckbytes.simmodemapi.modem

enum class PduAlphabet(
  val bitPattern: Int
) {
  GSM_SEVEN_BIT(0b00000000),
  EIGHT_BIT(0b00000100),
  UCS2_SIXTEEN_BIT(0b00001000),
  RESERVED(0b00001100)
}