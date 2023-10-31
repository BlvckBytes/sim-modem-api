package me.blvckbytes.simmodemapi.domain.pdu.header

class UserDataHeader(
  vararg elements: InformationElement
) : ArrayList<InformationElement>() {

  init {
    addAll(elements)
  }

  fun getConcatenatedShortMessage(): ConcatenatedShortMessage? {
    return firstOrNull { InformationElementIdentifier.CONCATENATED_SHORT_MESSAGE.isInstance(it) } as ConcatenatedShortMessage?
  }
}