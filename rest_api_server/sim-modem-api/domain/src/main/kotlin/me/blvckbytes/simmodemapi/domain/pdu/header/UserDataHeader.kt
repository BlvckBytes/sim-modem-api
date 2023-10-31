package me.blvckbytes.simmodemapi.domain.pdu.header

import kotlin.reflect.cast

class UserDataHeader(vararg elements: InformationElement) {

  private val elementByType = mutableMapOf<InformationElementIdentifier<*>, InformationElement>()

  init {
    for (element in elements)
      addElement(element)
  }

  val elements get() = elementByType.values

  fun addElement(element: InformationElement) {
    elementByType[element.getType()] = element
  }

  fun <T: InformationElement> getElement(type: InformationElementIdentifier<T>): T? {
    val element = elementByType[type] ?: return null
    return type.wrapperClass.cast(element)
  }
}