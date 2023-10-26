package me.blvckbytes.simmodemapi.domain.exception

class MessageTooLongException(
  val maximumCharacterLength: Int
) : RuntimeException()