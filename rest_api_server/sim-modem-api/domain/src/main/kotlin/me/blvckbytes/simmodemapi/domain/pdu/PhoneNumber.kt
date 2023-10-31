package me.blvckbytes.simmodemapi.domain.pdu

import java.util.EnumSet

class PhoneNumber(
  val type: EnumSet<BinaryTypeOfAddressFlag>,
  val number: String
) {
  companion object {
    fun fromInternationalISDN(number: String): PhoneNumber {
      return PhoneNumber(
        EnumSet.of(
          BinaryTypeOfAddressFlag.TYPE_INTERNATIONAL,
          BinaryTypeOfAddressFlag.NUMBERING_PLAN_ISDN
        ),
        number
      )
    }
  }

  fun toTypeFlag(): Int {
    return type.fold(0x00) { accumulator, current ->
      current.apply(accumulator)
    }
  }
}