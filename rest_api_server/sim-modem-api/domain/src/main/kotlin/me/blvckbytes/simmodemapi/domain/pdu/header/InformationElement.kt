package me.blvckbytes.simmodemapi.domain.pdu.header

interface InformationElement {

  fun isContentEqualTo(other: InformationElement): Boolean

  fun getLengthInBytes(): Int

  fun write(output: MutableList<Byte>)

}