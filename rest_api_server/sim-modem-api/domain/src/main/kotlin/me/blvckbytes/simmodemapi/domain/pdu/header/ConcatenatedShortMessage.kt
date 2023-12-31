package me.blvckbytes.simmodemapi.domain.pdu.header

data class ConcatenatedShortMessage(
  var messageReferenceNumber: Int?,
  var totalNumberOfParts: Int,
  var sequenceNumberOfThisPart: Int
) : InformationElement {

  init {
    if (messageReferenceNumber == 0)
      throw IllegalStateException("Value zero is reserved, please use NULL for auto-generation")
  }

  override fun isContentEqualTo(other: InformationElement): Boolean {
    if (other !is ConcatenatedShortMessage)
      return false

    if (other.messageReferenceNumber != this.messageReferenceNumber)
      return false

    if (other.totalNumberOfParts != this.totalNumberOfParts)
      return false

    return other.sequenceNumberOfThisPart == this.sequenceNumberOfThisPart
  }

  override fun getLengthInBytes(): Int {
    return 5
  }

  override fun write(output: MutableList<Byte>) {
    output.add(InformationElementIdentifier.CONCATENATED_SHORT_MESSAGE.identifier.toByte())
    output.add(3)
    output.add((messageReferenceNumber ?: 0).toByte())
    output.add(totalNumberOfParts.toByte())
    output.add(sequenceNumberOfThisPart.toByte())
  }
}