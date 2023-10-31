package me.blvckbytes.simmodemapi.domain.pdu.header

interface InformationElement {

  fun getType(): InformationElementIdentifier<*>

  fun isContentEqualTo(other: InformationElement): Boolean

}