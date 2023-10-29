package me.blvckbytes.simmodemapi.domain.header

interface InformationElement {

  fun getType(): InformationElementIdentifier<*>

  fun isContentEqualTo(other: InformationElement): Boolean

}