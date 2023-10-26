package me.blvckbytes.simmodemapi.modem

import kotlin.reflect.KClass

sealed class InformationElementIdentifier<T: InformationElement>(
  val identifier: Byte,
  val wrapperClass: KClass<T>
) {
  object CONCATENATED_SHORT_MESSAGE: InformationElementIdentifier<ConcatenatedShortMessage>(0x00, ConcatenatedShortMessage::class)
}