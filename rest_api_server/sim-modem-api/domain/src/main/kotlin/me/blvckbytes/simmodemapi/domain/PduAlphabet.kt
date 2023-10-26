package me.blvckbytes.simmodemapi.domain

enum class PduAlphabet(
  val bitPattern: Int,
  val maximumCharacters: Int
) {
  // 1120 bytes max length for UD (User Data)

  GSM_SEVEN_BIT(0b00000000, 1120 / 7),
  EIGHT_BIT(0b00000100, 1120 / 8),
  UCS2_SIXTEEN_BIT(0b00001000, 1120 / 16),
  RESERVED(0b00001100, -1)
  ;

  companion object {
    val AVAILABLE_ALPHABETS_ASCENDING = arrayOf(
      GSM_SEVEN_BIT,
      UCS2_SIXTEEN_BIT
    )
  }
}