package me.blvckbytes.simmodemapi.domain

class PDU(
  val smsCenter: PhoneNumber?,
  val messageFlags: MessageFlags,
  val messageReferenceNumber: Int?
)