package me.blvckbytes.simmodemapi.domain.exception

class InvalidPduException(
  val reason: PduInvalidityReason,
  val description: String? = null
) : RuntimeException()