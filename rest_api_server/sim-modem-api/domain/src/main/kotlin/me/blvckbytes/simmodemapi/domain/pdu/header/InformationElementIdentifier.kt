package me.blvckbytes.simmodemapi.domain.pdu.header

import kotlin.reflect.KClass

enum class InformationElementIdentifier(
  val identifier: Int,
  private val wrapper: KClass<*>
) {
  CONCATENATED_SHORT_MESSAGE(0, ConcatenatedShortMessage::class)

  ;

  fun isInstance(value: Any): Boolean {
    return wrapper.isInstance(value)
  }
}