package me.blvckbytes.simmodemapi.rest.config

class DescribedInternalException(
  val description: String,
  val occurredException: java.lang.Exception?
) : RuntimeException()
