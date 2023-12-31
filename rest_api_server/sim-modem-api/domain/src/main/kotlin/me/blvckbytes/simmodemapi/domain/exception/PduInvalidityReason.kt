package me.blvckbytes.simmodemapi.domain.exception

enum class PduInvalidityReason {
  SHORTER_THAN_EXPECTED,
  MALFORMED_SMSC_NUMBER,
  MALFORMED_DESTINATION_NUMBER,
  INVALID_MESSAGE_TYPE,
  INVALID_VALIDITY_PERIOD_FORMAT,
  INVALID_ALPHABET,
  INVALID_INFORMATION_ELEMENT_IDENTIFIER,
  IEI_PARAMETER_COUNT_MISMATCH,
  HEADER_TOO_LONG,
  HEADER_TOO_SHORT,
  INVALID_MESSAGE
}