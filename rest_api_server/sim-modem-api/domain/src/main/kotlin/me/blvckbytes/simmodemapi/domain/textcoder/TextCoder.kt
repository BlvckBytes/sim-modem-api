package me.blvckbytes.simmodemapi.domain.textcoder

interface TextCoder {

  fun encode(value: String): ByteArray

  fun decode(value: ByteArray): String?

}