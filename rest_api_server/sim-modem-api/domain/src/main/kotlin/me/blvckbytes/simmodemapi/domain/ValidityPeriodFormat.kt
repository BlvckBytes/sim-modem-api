package me.blvckbytes.simmodemapi.domain

enum class ValidityPeriodFormat(
  private val value: Int
) {
  NOT_PRESENT     (0b000_00_000),
  RELATIVE_INTEGER(0b000_10_000),
  ABSOLUTE_INTEGER(0b000_11_000)
  ;

  companion object {
    private const val BITMASK = 0b000_11_000

    fun fromMessageFlagsValue(messageType: MessageType, value: Int): ValidityPeriodFormat? {
      for (type in values()) {
        if (type.isSet(messageType, value))
          return type
      }
      return null
    }
  }

  fun isSet(messageType: MessageType, value: Int): Boolean {
    if (messageType != MessageType.SMS_SUBMIT)
      throw IllegalStateException("This field doesn't apply to the message type of $messageType")

    return (value and BITMASK) == this.value
  }

  fun apply(messageType: MessageType, value: Int): Int {
    if (messageType != MessageType.SMS_SUBMIT)
      throw IllegalStateException("This field doesn't apply to the message type of $messageType")

    return BinaryUtils.setBits(value, this.value, BITMASK)
  }
}