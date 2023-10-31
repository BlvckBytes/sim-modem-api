package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.exception.IllegalCharacterException
import me.blvckbytes.simmodemapi.domain.pdu.*
import me.blvckbytes.simmodemapi.domain.pdu.header.InformationElement
import org.junit.jupiter.api.AssertionFailureBuilder
import org.junit.jupiter.api.Assertions.*

class PduValidator(direction: PDUDirection, text: String) {

  companion object {
    fun encodeMessage(message: String): MessageEncodingResult {
      for (currentEncoding in PDUAlphabet.AVAILABLE_ALPHABETS_ASCENDING) {
        try {
          return MessageEncodingResult(currentEncoding.textCoder.encode(message), message.length, currentEncoding)
        } catch (exception: IllegalCharacterException) {
          continue
        }
      }

      throw IllegalStateException("Could not encode the message $message")
    }
  }

  private val pdu: PDU

  init {
    this.pdu = parseText(direction, text)
  }

  fun assertSMSCAbsent(): PduValidator {
    assertNull(pdu.smsCenter)
    return this
  }

  fun assertSMSCPresent(number: String, vararg types: BinaryTypeOfAddressFlag): PduValidator {
    assertNotNull(pdu.smsCenter)
    assertEquals(number, pdu.smsCenter!!.number)

    ensureContainsExact(
      types.size, types::iterator,
      pdu.smsCenter!!.type.size, pdu.smsCenter!!.type::iterator,
    ) { a, b -> a == b }

    return this
  }

  fun assertMessageType(type: MessageType): PduValidator {
    assertEquals(type, pdu.messageFlags.messageType)
    return this
  }

  fun assertMessageReferenceNumberAbsent(): PduValidator {
    assertNull(pdu.messageReferenceNumber)
    return this
  }

  fun assertMessageReferenceNumberPresent(messageReferenceNumber: Int): PduValidator {
    assertEquals(messageReferenceNumber, pdu.messageReferenceNumber)
    return this
  }

  fun assertDestination(number: String, vararg types: BinaryTypeOfAddressFlag): PduValidator {
    assertEquals(number, pdu.destination.number)

    ensureContainsExact(
      types.size, types::iterator,
      pdu.destination.type.size, pdu.destination.type::iterator,
    ) { a, b -> a == b }

    return this
  }

  fun assertProtocolIdentifierFlagsExact(vararg flags: BinaryProtocolIdentifierFlag): PduValidator {
    ensureContainsExact(
      flags.size, flags::iterator,
      pdu.protocolIdentifierFlags.size, pdu.protocolIdentifierFlags::iterator
    ) { a, b -> a == b }
    return this
  }

  fun assertValidityPeriodFormatAbsent(): PduValidator {
    assertNull(pdu.messageFlags.validityPeriodFormat)
    return this
  }

  fun assertValidityPeriodFormatPresent(format: ValidityPeriodFormat): PduValidator {
    assertEquals(format, pdu.messageFlags.validityPeriodFormat)
    return this
  }

  fun assertValidityPeriodAbsent(): PduValidator {
    assertNull(pdu.validityPeriodUnit)
    return this
  }

  fun assertValidityPeriod(unit: ValidityPeriodUnit, value: Double): PduValidator {
    assertEquals(unit, pdu.validityPeriodUnit)
    assertEquals(value, pdu.validityPeriodValue)
    return this
  }

  fun assertMessageFlagsExact(vararg flags: BinaryMessageFlag): PduValidator {
    ensureContainsExact(
      flags.size, flags::iterator,
      pdu.messageFlags.binaryFlags.size, pdu.messageFlags.binaryFlags::iterator
    ) { a, b -> a == b }

    return this
  }

  fun assertDCSFlagsExact(vararg flags: BinaryDCSFlag): PduValidator {
    ensureContainsExact(
      flags.size, flags::iterator,
      pdu.dcsFlags.size, pdu.dcsFlags::iterator
    ) { a, b -> a == b }

    return this
  }

  fun assertHeaderAbsent(): PduValidator {
    assertNull(pdu.header)
    return this
  }

  fun assertHeaderContainsExact(vararg informationElements: InformationElement): PduValidator {
    val headerElements = pdu.header!!.elements

    ensureContainsExact(
      informationElements.size, informationElements::iterator,
      headerElements.size, headerElements::iterator
    ) { a, b -> a.isContentEqualTo(b) }

    return this
  }

  fun assertMessage(message: String): PduValidator {
    assertEquals(message, pdu.message)
    return this
  }

  private fun <T> ensureContainsExact(
    sizeExpected: Int,
    itemsExpectedIterator: () -> Iterator<T>,
    sizeActual: Int,
    itemsActualIterator: () -> Iterator<T>,
    comparisonFunction: (a: T, b: T) -> Boolean
  ) {
    if (sizeExpected != sizeActual) {
      val expectedString = itemsExpectedIterator().asSequence().joinToString(",", "[", "]")
      val actualString = itemsActualIterator().asSequence().joinToString(",", "[", "]")

      AssertionFailureBuilder.assertionFailure()
        .expected(sizeExpected)
        .actual(sizeActual)
        .reason(
          """
            Items did not match in size
            expected=$expectedString
            actual=$actualString
          """.trimIndent()
        )
        .buildAndThrow()
    }

    var itemsExpectedCounter = 0
    val expectedIterator = itemsExpectedIterator()

    expectedIterator@ while (expectedIterator.hasNext()) {
      val expectedItem = expectedIterator.next()
      val actualIterator = itemsActualIterator()

      while (actualIterator.hasNext()) {
        val currentActualItem = actualIterator.next()

        if (comparisonFunction(expectedItem, currentActualItem))
          continue@expectedIterator
      }

      val expectedString = itemsExpectedIterator().asSequence().joinToString(",", "[", "]")
      val actualString = itemsActualIterator().asSequence().joinToString(",", "[", "]")

      AssertionFailureBuilder.assertionFailure()
        .expected(expectedItem)
        .actual(null)
        .reason(
          """
            Expected item index=$itemsExpectedCounter did not find a match
            expectedItems=$expectedString
            actualItems=$actualString
          """.trimIndent()
        )
        .buildAndThrow()

      ++itemsExpectedCounter
    }
  }

  private fun parseText(direction: PDUDirection, text: String): PDU {
    return PDUReadHelper.parsePdu(
      direction,
      normalizeText(text).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    )
  }

  private fun normalizeText(text: String): String {
    val result = StringBuilder()

    var skipUntilNewline = false
    for (char in text) {
      if (char == ' ' || char == '\r')
        continue

      if (char == '#') {
        skipUntilNewline = true
        continue
      }

      if (char == '\n') {
        skipUntilNewline = false
        continue
      }

      if (skipUntilNewline)
        continue

      result.append(char)
    }

    return result.toString()
  }
}