package me.blvckbytes.simmodemapi.domain.textcoder

object EightBitTextCoder : TextCoder {

  /*
     8-bit data encoding mode treats the information as raw data. According to the standard,
     the alphabet for this encoding is user-specific.

     NOTE: Currently, there is absolutely no use for this encoding, which is why there's a
     dummy encoder acting as a placeholder for future implementations.
   */

  override fun encode(value: String): ByteArray {
    TODO("Not yet implemented")
  }

  override fun decode(value: ByteArray): String? {
    TODO("Not yet implemented")
  }
}