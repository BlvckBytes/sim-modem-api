package me.blvckbytes.simmodemapi.modem

object AsciiTextCoder : TextCoder {

  override fun encode(value: String): ByteArray {
    return value.toByteArray(Charsets.US_ASCII)
  }
}