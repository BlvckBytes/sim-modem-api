package me.blvckbytes.simmodemapi.modem

interface TextCoder {

  fun encode(value: String): ByteArray

}