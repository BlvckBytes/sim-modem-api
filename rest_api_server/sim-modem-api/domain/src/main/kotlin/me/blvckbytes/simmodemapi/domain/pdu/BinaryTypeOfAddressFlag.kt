package me.blvckbytes.simmodemapi.domain.pdu

import me.blvckbytes.simmodemapi.domain.BinaryUtils
import java.util.*

enum class BinaryTypeOfAddressFlag(
  private val value: Int,
  private val bitmask: Int
) {
  /*
    TP-TOA (Type Of Address)
    Bit 7:       always 1
    Bit 6,5,4:   TON (Type Of Number)
    Bit 3,2,1,0: NPI (Numbering Plan Identification)
   */

  /*
    The type of number "unknown" is used when the user or the network
    has no knowledge of the type of number, e.g. international number,
    national number, etc. In this case the number digits field is
    organized according to the network dialling plan, e.g. prefix
    or escape digits might be present.
   */
  TYPE_UNKNOWN(0b10000000, 0b11110000),

  /*
    Prefix or escape digits shall not be included.
    The international format shall be accepted by the MSC when the
    call is destined to a destination in the same country as the MSC.
   */
  TYPE_INTERNATIONAL(0b10010000, 0b11110000),

  /*
    Prefix or escape digits shall not be included.
   */
  TYPE_NATIONAL(0b10100000, 0b11110000),

  /*
    This type of number is used to indicate a administration/service number
    specific to the serving network, e.g. used to access an operator.
   */
  TYPE_NETWORK_SPECIFIC(0b10110000, 0b11110000),

  /*
    Dedicated address, short code
   */
  TYPE_DEDICATED(0b11000000, 0b11110000),

  // 101, 110, 111 Reserved

  /*
    In the case of numbering plan "unknown", the number digits field is organized according
    to the network dialling plan; e.g. prefix or escape digits might be present.
   */
  NUMBERING_PLAN_UNKNOWN(0b10000000, 0b10001111),

  /*
    ISDN/telephony numbering plan (Rec. E.164/E.163)
   */
  NUMBERING_PLAN_ISDN(0b10000001, 0b10001111),

  /*
    Data numbering plan (Recommendation X.121)
   */
  NUMBERING_PLAN_DATA(0b10000011, 0b10001111),

  /*
    Telex numbering plan (Recommendation F.69)
   */
  NUMBERING_PLAN_TELEX(0b10000100, 0b10001111),

  /*
    National numbering plan
   */
  NUMBERING_PLAN_NATIONAL(0b10001000, 0b10001111),

  /*
    Private numbering plan
   */
  NUMBERING_PLAN_PRIVATE(0b10001001, 0b10001111)

  // 1111 Reserved
  ;

  companion object {
    fun fromTypeOfAddressValue(value: Int): EnumSet<BinaryTypeOfAddressFlag> {
      val result = EnumSet.noneOf(BinaryTypeOfAddressFlag::class.java)

      for (type in values()) {
        if (type.isSet(value))
          result.add(type)
      }

      return result
    }
  }

  fun isSet(value: Int): Boolean {
    return (value and this.bitmask) == this.value
  }

  fun apply(value: Int): Int {
    return BinaryUtils.setBits(value, this.value, this.bitmask)
  }
}