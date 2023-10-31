package me.blvckbytes.simmodemapi.domain.exception

class InvalidPduException(
  reason: PduInvalidityReason,
  description: String? = null
) : RuntimeException("Encountered invalid PDU due to $reason: ${description ?: "No Description provided"}")