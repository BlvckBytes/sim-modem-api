package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.*
import me.blvckbytes.simmodemapi.domain.pdu.*
import me.blvckbytes.simmodemapi.domain.pdu.header.ConcatenatedShortMessage
import me.blvckbytes.simmodemapi.domain.pdu.header.UserDataHeader
import org.junit.jupiter.api.Test
import java.util.*

class PDUWriteHelperTests {

  @Test
  fun `can write GSM-7, concat header, validity period`() {
    val encodingResult = PduValidator.encodeMessage("Hello, world! :^)")

    for (i in 1 .. 3) {
      val pduBytes = PDUWriteHelper.writePdu(
        PDU(
          PhoneNumber.fromInternationalISDN("43123456789"),
          MessageFlags(
            MessageType.SMS_SUBMIT,
            ValidityPeriodFormat.RELATIVE_INTEGER,
            EnumSet.of(
              BinaryMessageFlag.STATUS_REPORT_REQUEST,
              BinaryMessageFlag.HAS_USER_DATA_HEADER
            )
          ),
          23,
          PhoneNumber.fromInternationalISDN("43987654321"),
          BinaryProtocolIdentifierFlag.FOR_SENDING_SHORT_MESSAGE,
          BinaryDCSFlag.fromAlphabetForShortMessage(encodingResult.alphabet),
          ValidityPeriodUnit.WEEKS,
          5.0,
          UserDataHeader(
            ConcatenatedShortMessage(23, 3, i)
          ),
          ""
        ),
        encodingResult
      ).data

      PduValidator(PDUDirection.MS_TO_SC, BinaryUtils.binaryToHexString(pduBytes))
        .assertSMSCPresent(
          "43123456789",
          BinaryTypeOfAddressFlag.TYPE_INTERNATIONAL, BinaryTypeOfAddressFlag.NUMBERING_PLAN_ISDN
        )
        .assertMessageType(MessageType.SMS_SUBMIT)
        .assertMessageFlagsExact(BinaryMessageFlag.HAS_USER_DATA_HEADER, BinaryMessageFlag.STATUS_REPORT_REQUEST)
        .assertMessageReferenceNumberPresent(23)
        .assertDestination(
          "43987654321",
          BinaryTypeOfAddressFlag.TYPE_INTERNATIONAL, BinaryTypeOfAddressFlag.NUMBERING_PLAN_ISDN
        )
        .assertProtocolIdentifierFlagsExact(BinaryProtocolIdentifierFlag.MS_TO_SC_SHORT_MESSAGE)
        .assertDCSFlagsExact(BinaryDCSFlag.SEVEN_BIT_GSM_ALPHABET, BinaryDCSFlag.MESSAGE_CLASS_1)
        .assertValidityPeriodFormatPresent(ValidityPeriodFormat.RELATIVE_INTEGER)
        .assertValidityPeriod(ValidityPeriodUnit.WEEKS, 5.0)
        .assertHeaderContainsExact(ConcatenatedShortMessage(23, 3, i))
    }
  }
}