package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.exception.IllegalCharacterException
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.UnmappableCharacterException

object UCS2TextCoder {

  private val encoder = Charsets.UTF_16BE
    .newEncoder()
    .onMalformedInput(CodingErrorAction.REPORT)
    .onUnmappableCharacter(CodingErrorAction.REPORT)

  fun encode(value: String): ByteArray {
    try {
      val result = encoder.encode(CharBuffer.wrap(value))
      val byteArray = ByteArray(result.capacity())
      result.get(byteArray)
      return byteArray
    } catch (exception: UnmappableCharacterException) {
      throw IllegalCharacterException()
    }
  }
}