package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.PduAlphabet

class MessageEncodingResult(
  val bytes: ByteArray,
  val numberOfCharacters: Int,
  val alphabet: PduAlphabet
)