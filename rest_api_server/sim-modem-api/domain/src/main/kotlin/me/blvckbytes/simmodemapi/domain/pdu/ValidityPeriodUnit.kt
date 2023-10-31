package me.blvckbytes.simmodemapi.domain.pdu

enum class ValidityPeriodUnit(
  val min: Double,
  val max: Double,
  val step: Double
) {
  MINUTES(5.0, 720.0, 5.0),
  HOURS(12.5, 24.0, 0.5),
  DAYS(2.0, 30.0, 1.0),
  WEEKS(5.0, 63.0, 1.0)

  ;

  fun isValueWithinRange(value: Double): Boolean {
    return value >= min && value <= max && value % step == 0.00
  }
}