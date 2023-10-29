package me.blvckbytes.simmodemapi.domain

import java.util.EnumSet

enum class BinaryMessageFlag(
  private val value: Int,
  private val bitmask: Int,
  private vararg val messageTypes: MessageType
) {
  /*
    The TP-Reject-Duplicates is a 1-bit field located within bit 2 of the first octet of
    SMS-SUBMIT and has the following values:

    0 Instruct the SC to accept an SMS-SUBMIT for an SM still held in the SC which has the
      same TP-MR and the same TP-DA as a previously submitted SM from the same OA.

    1 Instruct the SC to reject an SMS-SUBMIT for an SM still held in the SC which has the
      same TP-MR and the same TP-DA as the previously submitted SM from the same OA. In this
      case an appropriate TP-FCS value will be returned in the SMS-SUBMIT-REPORT.
   */
  REJECT_DUPLICATES       (0b00000_1_00, 0b00000_1_00, MessageType.SMS_SUBMIT),

  /*
    The TP-More-Messages-to-Send is a 1-bit field, located within bit no. 2 of the first octet
    of SMS-DELIVER and SMS-STATUS-REPORT, and to be given the following values:

    0 More messages are waiting for the MS in this SC
    1 No more messages are waiting for the MS in this SC
   */
  MORE_MESSAGES_TO_SEND   (0b00000_0_00, 0b00000_1_00, MessageType.SMS_DELIVER, MessageType.SMS_STATUS_REPORT),

  /*
    The TP-Status-Report-Indication is a 1-bit field, located within bit no. 5 of the first octet
    of SMS-DELIVER, and to be given the following values:

    0 A status report will not be returned to the SME
    1 A status report will be returned to the SME
   */
  STATUS_REPORT_INDICATION(0b00_1_00000, 0b00_1_00000, MessageType.SMS_DELIVER),

  /*
    The TP-Status-Report-Request is a 1-bit field, located within bit no. 5 of the first octet
    of SMS-SUBMIT and SMS-COMMAND, and to be given the following values:

    0 A status report is not requested
    1 A status report is requested
   */
  STATUS_REPORT_REQUEST   (0b00_1_00000, 0b00_1_00000, MessageType.SMS_SUBMIT, MessageType.SMS_COMMAND),

  /*
    The TP-Status-Report-Qualifier is a 1 bit field located within bit 5 of the first octet
    of SMS-STATUS-REPORT and has the following values:

    0 The SMS-STATUS-REPORT is the result of a SMS-SUBMIT
    1 The SMS-STATUS-REPORT is the result of an SMS-COMMAND e.g.
      an Enquiry
   */
  STATUS_REPORT_QUALIFIER (0b00_1_00000, 0b00_1_00000, MessageType.SMS_STATUS_REPORT),

  /*
    The TP-User-Data-Header-Indicator is a 1 bit field within bit 6 of the first octet
    of an SMS-SUBMIT and SMS-DELIVER PDU and has the following values:

    0 The TP-UD field contains only the short message
    1 The beginning of the TP-UD field contains a Header in addition to the short message
   */
  HAS_USER_DATA_HEADER    (0b0_1_000000, 0b0_1_000000, MessageType.SMS_SUBMIT, MessageType.SMS_DELIVER),

  /*
    The TP-Reply-Path is a 1-bit field, located within bit no 7 of the first octet of both SMS-DELIVER and
    SMS-SUBMIT, and to be given the following values:

    0 TP-Reply-Path parameter is not set in this SMS-SUBMIT/DELIVER
    1 TP-Reply-Path parameter is set in this SMS-SUBMIT/DELIVER

    SMS_DELIVER: Parameter indicating that Reply Path exists.
    SMS_SUBMIT: Parameter indicating the request for Reply Path.
   */
  REPLY_PATH              (0b1_0000000, 0b1_0000000, MessageType.SMS_DELIVER, MessageType.SMS_SUBMIT)
  ;

  companion object {
    fun fromMessageFlagsValue(messageType: MessageType, value: Int): EnumSet<BinaryMessageFlag> {
      val result = EnumSet.noneOf(BinaryMessageFlag::class.java)

      for (type in values()) {
        if (!type.messageTypes.contains(messageType))
          continue

        if ((value and type.bitmask) == type.value)
          result.add(type)
      }

      return result
    }
  }
}