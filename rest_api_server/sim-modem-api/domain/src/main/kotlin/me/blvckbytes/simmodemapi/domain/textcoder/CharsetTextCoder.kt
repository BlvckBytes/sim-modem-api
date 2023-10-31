package me.blvckbytes.simmodemapi.domain.textcoder

import me.blvckbytes.simmodemapi.domain.exception.IllegalCharacterException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException

open class CharsetTextCoder(
  charset: Charset
) : TextCoder {

  private val encoder = charset
    .newEncoder()
    .onMalformedInput(CodingErrorAction.REPORT)
    .onUnmappableCharacter(CodingErrorAction.REPORT)

  private val decoder = charset
    .newDecoder()
    .onMalformedInput(CodingErrorAction.REPORT)
    .onUnmappableCharacter(CodingErrorAction.REPORT)

  override fun encode(value: String): ByteArray {
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

  override fun decode(value: ByteArray): String? {
    return try {
      val result = decoder.decode(ByteBuffer.wrap(value))
      result.toString()
    } catch (exception: Exception) {
      null
    }
  }
}