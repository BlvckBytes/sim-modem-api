package me.blvckbytes.simmodemapi.domain.header

import me.blvckbytes.simmodemapi.domain.PduAlphabet
import kotlin.reflect.cast

class UserDataHeader {

  private val elementByType = mutableMapOf<InformationElementIdentifier<*>, InformationElement>()

  fun addElement(element: InformationElement) {
    elementByType[element.getType()] = element
  }

  fun <T: InformationElement> getElement(type: InformationElementIdentifier<T>): T? {
    val element = elementByType[type] ?: return null
    return type.wrapperClass.cast(element)
  }

  fun write(output: MutableList<Byte>): Int {
    if (elementByType.isEmpty())
      return 0

    val previousLength = output.size

    for (element in elementByType.values)
      element.write(output)

    output.add(previousLength, (output.size - previousLength).toByte())

    return output.size - previousLength
  }

  fun getLengthInBytes(): Int {
    if (elementByType.isEmpty())
      return 0

    var length = 1

    for (element in elementByType.values)
      length += element.getLengthInBytes()

    return length
  }

  fun getNumberOfTakenUpCharacters(alphabet: PduAlphabet): Int {
    val byteLength = getLengthInBytes()

    return when (alphabet) {
      PduAlphabet.GSM_SEVEN_BIT -> (byteLength * 8 + (7 - 1)) / 7
      PduAlphabet.EIGHT_BIT -> byteLength
      PduAlphabet.UCS2_SIXTEEN_BIT -> (byteLength * 8 + (16 - 1)) / 16
      else -> throw IllegalStateException("Unsupported alphabet")
    }
  }
}