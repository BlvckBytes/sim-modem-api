package me.blvckbytes.simmodemapi.domain

object BinaryUtils {

  fun binaryToHexString(buffer: ByteArray): String {
    val result = StringBuilder()
    for (byte in buffer)
      result.append("%02X".format(byte))
    return result.toString()
  }

  fun nLsbMask(n: Int): Int {
    return when (n) {
      0 -> 0b00000000
      1 -> 0b00000001
      2 -> 0b00000011
      3 -> 0b00000111
      4 -> 0b00001111
      5 -> 0b00011111
      6 -> 0b00111111
      7 -> 0b01111111
      else -> throw IllegalStateException("Invalid n-lsb-mask for n=$n requested")
    }
  }

  fun nMsbMask(n: Int): Int {
    return when (n) {
      0 -> 0b00000000
      1 -> 0b10000000
      2 -> 0b11000000
      3 -> 0b11100000
      4 -> 0b11110000
      5 -> 0b11111000
      6 -> 0b11111100
      7 -> 0b11111110
      else -> throw IllegalStateException("Invalid n-msb-mask for n=$n requested")
    }
  }

  fun setBits(destination: Int, source: Int, bitmask: Int): Int {
    TODO()
  }
}