package me.blvckbytes.simmodemapi.domain

import java.util.EnumSet

class MessageFlags(
  val messageType: MessageType,
  val validityPeriodFormat: ValidityPeriodFormat?,
  val binaryFlags: EnumSet<BinaryMessageFlag>
)