package me.blvckbytes.simmodemapi.domain

enum class TypeOfAddress {
  /*
    TP-TOA (Type Of Address)
    Bit 7:       always 1

    Bit 6,5,4:   TON (Type Of Number)
    0 0 0 Unknown
          The type of number "unknown" is used when the user or the network
          has no knowledge of the type of number, e.g. international number,
          national number, etc. In this case the number digits field is
          organized according to the network dialling plan, e.g. prefix
          or escape digits might be present.
    0 0 1 International number
          Prefix or escape digits shall not be included.
          The international format shall be accepted by the MSC when the
          call is destined to a destination in the same country as the MSC.
    0 1 0 National number
          Prefix or escape digits shall not be included.
    0 1 1 Network specific number
          This type of number is used to indicate a administration/service number
          specific to the serving network, e.g. used to access an operator.
    1 0 0 Dedicated address, short code
    1 0 1 Reserved
    1 1 0 Reserved
    1 1 1 Reserved for extension

    Bit 3,2,1,0: NPI (Numbering Plan Identification)
    0 0 0 0 Unknown
    0 0 0 1 ISDN/telephony numbering plan (Rec. E.164/E.163)
    0 0 1 1 Data numbering plan (Recommendation X.121)
    0 1 0 0 Telex numbering plan (Recommendation F.69)
    1 0 0 0 National numbering plan
    1 0 0 1 Private numbering plan
    1 1 1 1 Reserved for extension
   */
}