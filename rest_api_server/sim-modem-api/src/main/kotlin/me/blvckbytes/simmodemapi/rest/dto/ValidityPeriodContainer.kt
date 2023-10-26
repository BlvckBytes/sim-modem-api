package me.blvckbytes.simmodemapi.rest.dto

import me.blvckbytes.simmodemapi.modem.ValidityPeriodUnit

interface ValidityPeriodContainer {
  val validityPeriodUnit: ValidityPeriodUnit?
  val validityPeriodValue: Double?
}