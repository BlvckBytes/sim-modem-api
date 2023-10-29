package me.blvckbytes.simmodemapi.modem.coder

object ASCIITextCoder {

  fun encode(value: String): ByteArray {
    return value.toByteArray(Charsets.US_ASCII)
  }
}