package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.*
import me.blvckbytes.simmodemapi.domain.header.ConcatenatedShortMessage
import org.junit.jupiter.api.Test

class PDUReadHelperTests {

  @Test
  fun `can read GSM-7, no header, with SMSC`() {
    PduValidator(PDUDirection.MS_TO_SC, """
      07 91 5892000000F0 # SMSC 85290000000
      01 # SUBMIT
      00 # No reference number
      0B 91 5892214365F70 # Destination 85291234567
      00 # Protocol identifier
      02 # Data coding scheme
      14 # 14 units long message
      93A283D0795C3F33C88FE06CDCB6E32885EC6D341EDF27C1E3E97E72E
    """)
      .assertSMSCPresent(
        "85290000000",
        BinaryTypeOfAddressFlag.TYPE_INTERNATIONAL, BinaryTypeOfAddressFlag.NUMBERING_PLAN_ISDN
      )
      .assertMessageType(MessageType.SMS_SUBMIT)
      .assertMessageReferenceNumberAbsent()
      .assertDestination(
        "85291234567",
        BinaryTypeOfAddressFlag.TYPE_INTERNATIONAL, BinaryTypeOfAddressFlag.NUMBERING_PLAN_ISDN
      )
      .assertProtocolIdentifier(0x00)
      .assertValidityPeriodFormatPresent(ValidityPeriodFormat.NOT_PRESENT)
      .assertMessageFlagsExact()
      .assertDCSFlagsExact(BinaryDCSFlag.SEVEN_BIT_GSM_ALPHABET)
      .assertValidityPeriodAbsent()
      .assertHeaderAbsent()
      .assertMessage("It is easy to send text messages.")
  }

  @Test
  fun `can read GSM-7, no header, without SMSC`() {
    PduValidator(PDUDirection.MS_TO_SC, """
      00 # Default SMSC
      01 # SUBMIT
      00 # No reference number
      0B 91 5892214365F70 # Destination 85291234567
      00 # Protocol identifier
      02 # Data coding scheme
      14 # 14 units long message
      93A283D0795C3F33C88FE06CDCB6E32885EC6D341EDF27C1E3E97E72E
    """)
      .assertSMSCAbsent()
      .assertMessageType(MessageType.SMS_SUBMIT)
      .assertMessageReferenceNumberAbsent()
      .assertDestination(
        "85291234567",
        BinaryTypeOfAddressFlag.TYPE_INTERNATIONAL, BinaryTypeOfAddressFlag.NUMBERING_PLAN_ISDN
      )
      .assertProtocolIdentifier(0x00)
      .assertValidityPeriodFormatPresent(ValidityPeriodFormat.NOT_PRESENT)
      .assertMessageFlagsExact()
      .assertDCSFlagsExact(BinaryDCSFlag.SEVEN_BIT_GSM_ALPHABET)
      .assertValidityPeriodAbsent()
      .assertHeaderAbsent()
      .assertMessage("It is easy to send text messages.")
  }

  @Test
  fun `can read UCS2 and GSM-7, concat header, validity period`() {
    val pduValues = listOf(
      """
        08 91 345600090000F0 # SMSC 4365009000000
        71 # SUBMIT, USER_DATA_HEADER, STATUS_REPORT
        00 # No reference number
        0D 91 342143658709F1 # Destination 4312345678901
        00 # Protocol identifier
        11 # Data coding scheme, GSM7, CLASS_1
        00 # Validity period of 5 minutes
        A0 # 160 units long message
        05 # 5 bytes long header
        00 03 00 03 01 # Concat, 1/3
        98 6F79B90D4AC3E7F53688FC66BFE5A0799A0E0AB7CB741668FC76CFCBF432BD2E07CDC3E4347C3E4EBBCFA0323B4D97B340F33219444E87DB20F7DB5D6FE741E5B4BCFD2683E8E536FC2D07A5DDF634B9EEA683EA74103B2C7ECBCBA0321D447EB3DFF232A81D3EBBC3A0303B1DAFE7C36D50591EA6B340F33219444E87DB20FB9B5D87D3EB1A
      """,

      // The next two PDUs are analogous to the first one and just have different concatenation IDs in their headers

      // 2/3
      "0891345600090000F071000D91342143658709F1001100A0050003000302C22E50900EB297E56F50F93D0795E9A0F0785C9F87DBA0321DA4AECFE96F10B9FE0691DFECB7BC3C0795E9A07218242F8BEB6D17684A2FD34163769A1E06ADC37332E85C1797E56779D9CD02B9DFA079390CA287D7E976981E06CDC3EE31BD3E0795E77410F32D2FB74169F8BCDE0691DFECB71C344FD341E17699EE0231DFF2721B9486CFEB1A",

      // 3/3
      "0891345600090000F071000D91342143658709F100110067050003000303DA20F29BFD9683E6693A28DC2ED359A0F1DB3D2FD3CBF4BA1C340F93D3F0F938ED3E83CAEC345DCE02CDCB6410391D6E83DC6F77BD9D0795D3F2F69B0CA297DBF0B71C9476DBD3E4BA9B0EAAD341ECB0F82D2FBB1A"
    )

    pduValues.forEachIndexed { index, pduValue ->
      PduValidator(PDUDirection.MS_TO_SC, pduValue)
        .assertSMSCPresent(
          "4365009000000",
          BinaryTypeOfAddressFlag.TYPE_INTERNATIONAL, BinaryTypeOfAddressFlag.NUMBERING_PLAN_ISDN
        )
        .assertMessageType(MessageType.SMS_SUBMIT)
        .assertMessageFlagsExact(BinaryMessageFlag.HAS_USER_DATA_HEADER, BinaryMessageFlag.STATUS_REPORT_REQUEST)
        .assertMessageReferenceNumberAbsent()
        .assertDestination(
          "4312345678901",
          BinaryTypeOfAddressFlag.TYPE_INTERNATIONAL, BinaryTypeOfAddressFlag.NUMBERING_PLAN_ISDN
        )
        .assertProtocolIdentifier(0x00)
        .assertDCSFlagsExact(BinaryDCSFlag.SEVEN_BIT_GSM_ALPHABET, BinaryDCSFlag.MESSAGE_CLASS_1)
        .assertValidityPeriodFormatPresent(ValidityPeriodFormat.RELATIVE_INTEGER)
        .assertValidityPeriod(ValidityPeriodUnit.MINUTES, 5.0)
        .assertHeaderContainsExact(ConcatenatedShortMessage(null, 3, index + 1))
    }
  }
}