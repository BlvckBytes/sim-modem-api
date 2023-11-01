package me.blvckbytes.simmodemapi.domain.textcoder

object ASCIITextCoder : CharsetTextCoder(Charsets.US_ASCII) {

  private val CONTROL_CHARACTERS = (0..31).map { it.toChar() }

  fun trimControlCharacters(input: String): String {
    return input.trim { it in CONTROL_CHARACTERS }
  }

  fun substituteUnprintableAscii(value: String): String {
    val valueCharacters = value.toCharArray()
    val result = StringBuilder()

    for (char in valueCharacters) {
      if (char.code >= 32) {
        result.append(char)
        continue
      }

      result.append('\\')

      when (char) {
        '\n' -> result.append('n')
        '\r' -> result.append('r')
        '\t' -> result.append('t')
        else -> result.append("x%02X".format(char.code))
      }
    }

    return result.toString()
  }
}