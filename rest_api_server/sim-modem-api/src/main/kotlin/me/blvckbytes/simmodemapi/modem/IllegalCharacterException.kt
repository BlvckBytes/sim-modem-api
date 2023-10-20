package me.blvckbytes.simmodemapi.modem

class IllegalCharacterException(
  val character: Char
) : RuntimeException("Illegal character occurred: '$character'")