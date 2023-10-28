package me.blvckbytes.simmodemapi.domain

import java.util.EnumSet

class PDU(
  val smsCenter: PhoneNumber?,
  val messageFlags: MessageFlags,
  val messageReferenceNumber: Int?,
  val destination: PhoneNumber,
  val protocolIdentifier: Int,
  val dcsFlags: EnumSet<BinaryDCSFlag>
)