package me.blvckbytes.simmodemapi.rest.dto

import me.blvckbytes.simmodemapi.domain.pdu.ValidityPeriodUnit

interface ValidityPeriodContainer {
  val validityPeriodUnit: ValidityPeriodUnit?
  val validityPeriodValue: Double?
}