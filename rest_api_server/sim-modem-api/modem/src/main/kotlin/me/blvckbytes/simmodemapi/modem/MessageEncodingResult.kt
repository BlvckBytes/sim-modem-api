package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.pdu.PDUAlphabet

class MessageEncodingResult(
  val bytes: ByteArray,
  val numberOfCharacters: Int,
  val alphabet: PDUAlphabet
)