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
) {
  fun toDCSFlag(): Int {
    return dcsFlags.fold(0x00) { accumulator, current ->
      current.apply(accumulator)
    }
  }

  fun toProtocolIdentifierFlag(): Int {
    return protocolIdentifierFlags.fold(0x00) { accumulator, current ->
      current.apply(accumulator)
    }
  }

  fun toRelativeValidityPeriodValue(): Int {
    /*
      This byte indicates for how long this message is still valid to deliver to the
      recipient, relative to when the SC received it.

      TP-VP value  Validity period value
      0 to 143     (TP-VP + 1) x 5 minutes (i.e. 5 minutes intervals up to 12 hours)
      144 to 167   12 hours + ((TP-VP -143) x 30 minutes)
      168 to 196   (TP-VP - 166) x 1 day
      197 to 255   (TP-VP - 192) x 1 week

      =>

      0-143: 5, 10, 15, ... 715, 720 (12h)
      144-167: 12.5h, 13h, 13.5h, ... 23.5h, 24h
      168-196: 2d, 3d, 4d, ... 29d, 30d
      197-255: 5w, 6w, 7w, ..., 62w, 63w
     */

    val value = when (validityPeriodUnit) {
      ValidityPeriodUnit.MINUTES -> ((validityPeriodValue / 5) - 1).toInt()
      ValidityPeriodUnit.HOURS -> ((validityPeriodValue - 12) * 2 + 143).toInt()
      ValidityPeriodUnit.DAYS -> (validityPeriodValue + 166).toInt()
      ValidityPeriodUnit.WEEKS -> (validityPeriodValue + 192).toInt()
      null -> throw IllegalStateException("No validity period unit specified")
    }

    if (!validityPeriodUnit.isValueWithinRange(validityPeriodValue))
      throw IllegalStateException("Value $validityPeriodValue is not within range of $validityPeriodUnit")

    return value
  }
}