package me.blvckbytes.simmodemapi.domain.header

interface InformationElement {

  fun write(output: MutableList<Byte>)

  fun getLengthInBytes(): Int

  fun getType(): InformationElementIdentifier<*>

  fun isContentEqualTo(other: InformationElement): Boolean

}