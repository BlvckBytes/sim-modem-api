package me.blvckbytes.simmodemapi.domain

import me.blvckbytes.simmodemapi.domain.header.UserDataHeader
import java.util.EnumSet

class PDU(
  val smsCenter: PhoneNumber?,
  val messageFlags: MessageFlags,
  val messageReferenceNumber: Int?,
  val destination: PhoneNumber,
  val protocolIdentifierFlags: EnumSet<BinaryProtocolIdentifierFlag>,
  val dcsFlags: EnumSet<BinaryDCSFlag>,
  val validityPeriodUnit: ValidityPeriodUnit?,
  val validityPeriodValue: Double,
  val header: UserDataHeader?,
  val message: String
)