package me.blvckbytes.simmodemapi.modem

class MessageTooLongException(
  val maximumCharacterLength: Int
) : RuntimeException()