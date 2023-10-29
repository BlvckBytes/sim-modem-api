package me.blvckbytes.simmodemapi.domain.header

class ConcatenatedShortMessage(
  var messageReferenceNumber: Int?,
  var totalNumberOfParts: Int,
  var sequenceNumberOfThisPart: Int
) : InformationElement {

  init {
    if (messageReferenceNumber == 0)
      throw IllegalStateException("Value zero is reserved, please use NULL for auto-generation")
  }

  override fun getType(): InformationElementIdentifier<*> {
    return InformationElementIdentifier.CONCATENATED_SHORT_MESSAGE
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

  override fun toString(): String {
    return "ConcatenatedShortMessage(messageReferenceNumber=$messageReferenceNumber, totalNumberOfParts=$totalNumberOfParts, sequenceNumberOfThisPart=$sequenceNumberOfThisPart)"
  }
}