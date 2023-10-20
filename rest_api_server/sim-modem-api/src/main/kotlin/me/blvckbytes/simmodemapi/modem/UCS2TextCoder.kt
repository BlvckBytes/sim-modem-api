package me.blvckbytes.simmodemapi.modem

object UCS2TextCoder {

  fun encode(value: String): ByteArray {
    // TODO: Does this throw on unencodable characters? It should!
    return value.toByteArray(Charsets.UTF_16BE)
  }
}