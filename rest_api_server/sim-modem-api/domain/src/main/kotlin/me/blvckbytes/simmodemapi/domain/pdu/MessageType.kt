package me.blvckbytes.simmodemapi.domain.pdu

import me.blvckbytes.simmodemapi.domain.BinaryUtils

enum class MessageType(
  val direction: PDUDirection,
  private val value: Int
) {
  SMS_DELIVER       (PDUDirection.SC_TO_MS, 0b000000_00),
  SMS_DELIVER_REPORT(PDUDirection.MS_TO_SC, 0b000000_00),
  SMS_STATUS_REPORT (PDUDirection.SC_TO_MS, 0b000000_10),
  SMS_COMMAND       (PDUDirection.MS_TO_SC, 0b000000_10),
  SMS_SUBMIT        (PDUDirection.MS_TO_SC, 0b000000_01),
  SMS_SUBMIT_REPORT (PDUDirection.SC_TO_MS, 0b000000_01),
  // RESERVED 0b000000_11
  ;

  companion object {
    private const val BITMASK = 0b000000_11

    fun fromMessageFlagsValue(direction: PDUDirection, value: Int): MessageType? {
      for (type in values()) {
        if (type.isSet(direction, value))
          return type
      }
      return null
    }
  }

  fun isSet(direction: PDUDirection, value: Int): Boolean {
    return direction == this.direction && (value and BITMASK) == this.value
  }

  fun apply(direction: PDUDirection, value: Int): Int {
    if (direction != this.direction)
      throw IllegalStateException("The message type $this does not apply for the direction $direction")

    return BinaryUtils.setBits(value, this.value, BITMASK)
  }
}