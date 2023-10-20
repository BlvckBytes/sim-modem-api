package me.blvckbytes.simmodemapi.modem

object AsciiTextCoder {

  fun encode(value: String): ByteArray {
    return value.toByteArray(Charsets.US_ASCII)
  }
}