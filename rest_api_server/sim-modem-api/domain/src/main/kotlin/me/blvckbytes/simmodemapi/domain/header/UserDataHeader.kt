package me.blvckbytes.simmodemapi.domain.header

import kotlin.reflect.cast

class UserDataHeader {

  private val elementByType = mutableMapOf<InformationElementIdentifier<*>, InformationElement>()

  val elements get() = elementByType.values

  fun addElement(element: InformationElement) {
    elementByType[element.getType()] = element
  }

  fun <T: InformationElement> getElement(type: InformationElementIdentifier<T>): T? {
    val element = elementByType[type] ?: return null
    return type.wrapperClass.cast(element)
  }
}