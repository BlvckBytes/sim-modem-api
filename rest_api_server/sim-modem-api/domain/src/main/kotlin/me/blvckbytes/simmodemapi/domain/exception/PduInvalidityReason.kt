package me.blvckbytes.simmodemapi.domain.exception

enum class PduInvalidityReason {
  SHORTER_THAN_EXPECTED,
  MALFORMED_SMSC_NUMBER,
  MALFORMED_DESTINATION_NUMBER,
  INVALID_MESSAGE_TYPE,
  INVALID_VALIDITY_PERIOD_FORMAT
}