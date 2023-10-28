package me.blvckbytes.simmodemapi.domain

object BinaryUtils {

  fun binaryToHexString(buffer: ByteArray): String {
    val result = StringBuilder()
    for (byte in buffer)
      result.append("%02X".format(byte))
    return result.toString()
  }

  fun setBits(destination: Int, source: Int, bitmask: Int): Int {
    TODO()
  }
}