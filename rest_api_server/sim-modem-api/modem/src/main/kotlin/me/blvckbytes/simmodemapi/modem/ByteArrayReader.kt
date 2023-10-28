package me.blvckbytes.simmodemapi.modem

class ByteArrayReader(
  private val array: ByteArray
) {

  private val arrayLength = array.size
  private var nextIndex = 0

  val remainingBytes
    get() = arrayLength - nextIndex

  fun readInt(): Int {
    if (remainingBytes < 1)
      throw EndOfByteArrayException()

    return array[nextIndex++].toInt() and 0xFF
  }

  fun readBytes(numberOfBytes: Int): ByteArray {
    if (remainingBytes < numberOfBytes)
      throw EndOfByteArrayException()

    val result = array.sliceArray(nextIndex until nextIndex + numberOfBytes)
    nextIndex += numberOfBytes
    return result
  }
}