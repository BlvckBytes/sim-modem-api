package me.blvckbytes.simmodemapi.modem

interface InformationElement {

  fun write(output: MutableList<Byte>)

  fun getLengthInBytes(): Int

  fun getType(): InformationElementIdentifier<*>

}