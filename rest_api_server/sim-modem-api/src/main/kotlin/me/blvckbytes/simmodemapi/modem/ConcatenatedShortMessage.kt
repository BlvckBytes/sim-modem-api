package me.blvckbytes.simmodemapi.modem

class ConcatenatedShortMessage(
  var messageReferenceNumber: Int?,
  var totalNumberOfParts: Int,
  var sequenceNumberOfThisPart: Int
) : InformationElement {

  init {
    if (messageReferenceNumber == 0)
      throw IllegalStateException("Value zero is reserved, please use NULL for auto-generation")
  }

  override fun write(output: MutableList<Byte>) {
    output.add(InformationElementIdentifier.CONCATENATED_SHORT_MESSAGE.identifier)
    output.add(3)
    output.add((messageReferenceNumber ?: 0).toByte())
    output.add(totalNumberOfParts.toByte())
    output.add(sequenceNumberOfThisPart.toByte())
  }

  override fun getLengthInBytes(): Int {
    return 5
  }

  override fun getType(): InformationElementIdentifier<*> {
    return InformationElementIdentifier.CONCATENATED_SHORT_MESSAGE
  }
}