package me.blvckbytes.simmodemapi.domain

import java.util.*

enum class BinaryProtocolIdentifierFlag(
  private val value: Int,
  private val bitmask: Int
) {
  /*
    The Protocol-Identifier is the information element by which the SM-TL (Short Message Transfer Layer)
    either refers to the higher layer protocol being used, or indicates interworking with a certain type
    of telematic device.

    The Protocol-Identifier information element makes use of a particular field in the message types
    SMS-SUBMIT, SMS-DELIVER and SMS-COMMAND TP-Protocol-Identifier (TP-PID).

    The MS (Mobile Station) will not interpret reserved or unsupported values but shall store them as received.

    The SC (Service Center) may reject messages with a TP-Protocol-Identifier containing a reserved value or
    one which is not supported.
   */

  /*
    For the straightforward case of simple MS-to-SC short message transfer the Protocol
    Identifier is set to the value 0.
   */
  MS_TO_SC_SHORT_MESSAGE(0b00000000, 0b11111111),

  /*
    implicit - device type is specific to this SC, or can be concluded on the basis of the address
   */
  TELEMATIC_IMPLICIT(0b00100000, 0b11111111),

  /*
    telex (or teletex reduced to telex format)
   */
  TELEMATIC_TELEX(0b00100001, 0b11111111),

  /*
    group 3 telefax
   */
  TELEMATIC_TELEFAX_3(0b00100010, 0b11111111),

  /*
    group 4 telefax
   */
  TELEMATIC_TELEFAX_4(0b00100011, 0b11111111),

  /*
    voice telephone (i.e. conversion to speech)
   */
  TELEMATIC_VOICE(0b00100100, 0b11111111),

  /*
    ERMES (European Radio Messaging System)
   */
  TELEMATIC_ERMES(0b00100101, 0b11111111),

  /*
    National Paging system (known to the SC)
   */
  TELEMATIC_NATIONAL_PAGING(0b00100110, 0b11111111),

  /*
    Videotex (T.100/T.101)
   */
  TELEMATIC_VIDEOTEX(0b00100111, 0b11111111),

  /*
    teletex, carrier unspecified
   */
  TELEMATIC_TELETEX_UNSPECIFIED(0b00101000, 0b11111111),

  /*
    teletex, in PSPDN
   */
  TELEMATIC_TELETEX_PSPDN(0b00101001, 0b11111111),

  /*
    teletex, in CSPDN
   */
  TELEMATIC_TELETEX_CSPDN(0b00101010, 0b11111111),

  /*
    teletex, in analogue PSTN
   */
  TELEMATIC_TELETEX_PSTN(0b00101011, 0b11111111),

  /*
    teletex, in digital ISDN
   */
  TELEMATIC_TELETEX_ISDN(0b00101100, 0b11111111),

  /*
    UCI (Universal Computer Interface, ETSI DE/PS 3 01-3)
   */
  TELEMATIC_UCI(0b00101101, 0b11111111),

  // 01110..01111 reserved

  /*
    A message handling facility (known to the SC)
   */
  TELEMATIC_SC_KNOWN_MESSAGE_HANDLING(0b00110000, 0b11111111),

  /*
    Any public X.400-based message handling system
   */
  TELEMATIC_X400_MESSAGE_HANDLING(0b00110001, 0b11111111),

  /*
    Internet Electronic Mail
   */
  TELEMATIC_INTERNET_EMAIL(0b00110010, 0b11111111),

  // 10011..10111 reserved
  // 11000..11110 values specific to each SC, usage based on mutual agreement between the
  //              SME and the SC (7 combinations available for each SC)

  /*
    A GSM mobile station. The SC converts the SM from the received TP-Data-Coding-Scheme to any data
    coding scheme supported by that MS (e.g. Internet Electronic Mail the default).
   */
  TELEMATIC_GSM_MS(0b00111111, 0b11111111),

  /*
    Short Message Type 0
   */
  SHORT_MESSAGE_T0(0b01000000, 0b11111111),

  /*
    Replace Short Message Type 1
   */
  REPLACE_SHORT_MESSAGE_T1(0b01000001, 0b11111111),

  /*
    Replace Short Message Type 2
   */
  REPLACE_SHORT_MESSAGE_T2(0b01000010, 0b11111111),

  /*
    Replace Short Message Type 3
   */
  REPLACE_SHORT_MESSAGE_T3(0b01000011, 0b11111111),

  /*
    Replace Short Message Type 4
   */
  REPLACE_SHORT_MESSAGE_T4(0b01000100, 0b11111111),

  /*
    Replace Short Message Type 5
   */
  REPLACE_SHORT_MESSAGE_T5(0b01000101, 0b11111111),

  /*
    Replace Short Message Type 6
   */
  REPLACE_SHORT_MESSAGE_T6(0b01000110, 0b11111111),

  /*
    Replace Short Message Type 7
   */
  REPLACE_SHORT_MESSAGE_T7(0b01000111, 0b11111111),

  // 001000..011110 reserved

  /*
    Return Call Message
   */
  RETURN_CALL_MESSAGE(0b01011111, 0b11111111),

  // 100000..111110 reserved

  /*
    SIM Data download
   */
  SIM_DATA_DOWNLOAD(0b01111111, 0b11111111),

  ;

  companion object {
    val FOR_SENDING_SHORT_MESSAGE: EnumSet<BinaryProtocolIdentifierFlag> = EnumSet.of(MS_TO_SC_SHORT_MESSAGE)

    fun fromProtocolIdentifierValue(value: Int): EnumSet<BinaryProtocolIdentifierFlag> {
      val result = EnumSet.noneOf(BinaryProtocolIdentifierFlag::class.java)

      for (type in values()) {
        if (type.isSet(value))
          result.add(type)
      }

      return result
    }
  }

  fun isSet(value: Int): Boolean {
    return (value and this.bitmask) == this.value
  }

  fun apply(value: Int): Int {
    return BinaryUtils.setBits(value, this.value, this.bitmask)
  }
}