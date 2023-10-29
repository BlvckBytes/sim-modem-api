package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.exception.IllegalCharacterException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException

object UCS2TextCoder {

  private val encoder = Charsets.UTF_16BE
    .newEncoder()
    .onMalformedInput(CodingErrorAction.REPORT)
    .onUnmappableCharacter(CodingErrorAction.REPORT)

  private val decoder = Charsets.UTF_16BE
    .newDecoder()
    .onMalformedInput(CodingErrorAction.REPORT)
    .onUnmappableCharacter(CodingErrorAction.REPORT)

  fun encode(value: String): ByteArray {
    try {
      val result = encoder.encode(CharBuffer.wrap(value))
      val byteArray = ByteArray(result.capacity())
      result.get(byteArray)
      return byteArray
    } catch (exception: Exception) {
      when (exception) {
        is UnmappableCharacterException, is MalformedInputException -> throw IllegalCharacterException()
        else -> throw exception
      }
    }
  }

  fun decode(value: ByteArray): String? {
    return try {
      val result = decoder.decode(ByteBuffer.wrap(value))
      result.toString()
    } catch (exception: Exception) {
      null
    }
  }
}