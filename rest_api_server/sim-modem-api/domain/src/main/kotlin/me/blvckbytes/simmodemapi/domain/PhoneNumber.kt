package me.blvckbytes.simmodemapi.domain

import java.util.EnumSet

class PhoneNumber(
  val type: EnumSet<BinaryTypeOfAddressFlag>,
  val number: String
)