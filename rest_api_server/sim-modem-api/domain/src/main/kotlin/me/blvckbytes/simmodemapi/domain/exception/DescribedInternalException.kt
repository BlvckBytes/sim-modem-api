package me.blvckbytes.simmodemapi.domain.exception

class DescribedInternalException(
  val description: String,
  val occurredException: java.lang.Exception?
) : RuntimeException()
