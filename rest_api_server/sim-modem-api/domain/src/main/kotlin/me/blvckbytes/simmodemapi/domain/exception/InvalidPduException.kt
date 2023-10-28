package me.blvckbytes.simmodemapi.domain.exception

class InvalidPduException(
  val reason: PduInvalidityReason,
  val description: String? = null
) : RuntimeException("Encountered invalid PDU due to $reason: ${description ?: "No Description provided"}")