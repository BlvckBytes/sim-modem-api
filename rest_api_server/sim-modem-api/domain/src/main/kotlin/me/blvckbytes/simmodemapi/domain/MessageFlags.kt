package me.blvckbytes.simmodemapi.domain

import java.util.EnumSet

class MessageFlags(
  val messageType: MessageType,
  val validityPeriodFormat: ValidityPeriodFormat?,
  val binaryFlags: EnumSet<BinaryMessageFlag>
) {
  fun toFlag(): Int {
    var result = binaryFlags.fold(0x00) { accumulator, current ->
      current.apply(messageType, accumulator)
    }

    result = messageType.apply(messageType.direction, result)

    if (validityPeriodFormat != null)
      result = validityPeriodFormat.apply(messageType, result)

    return result
  }
}