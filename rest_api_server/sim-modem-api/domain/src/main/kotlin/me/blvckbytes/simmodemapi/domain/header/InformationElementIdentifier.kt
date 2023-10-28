package me.blvckbytes.simmodemapi.domain.header

import kotlin.reflect.KClass

sealed class InformationElementIdentifier<T: InformationElement>(
  val identifier: Int,
  val wrapperClass: KClass<T>
) {
  object CONCATENATED_SHORT_MESSAGE: InformationElementIdentifier<ConcatenatedShortMessage>(0x00, ConcatenatedShortMessage::class)
}