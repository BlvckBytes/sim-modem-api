package me.blvckbytes.simmodemapi.modem

class MessageEncodingResult(
  val bytes: ByteArray,
  val numberOfCharacters: Int,
  val alphabet: PduAlphabet
)