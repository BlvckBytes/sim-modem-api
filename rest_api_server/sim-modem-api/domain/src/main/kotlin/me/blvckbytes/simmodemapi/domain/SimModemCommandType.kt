package me.blvckbytes.simmodemapi.domain

enum class SimModemCommandType(
  val timeoutMs: Int,
  val requiredDelayFrom: (other: SimModemCommandType) -> Int
) {
  SEND_SMS_PROMPT(null, null),
  // SMS messages seem to get stuck for a long time when sent in bursts
  SEND_SMS_BODY(10_000, { if (it == SEND_SMS_BODY) 3500 else 0 }),
  SIGNAL_QUALITY(null, null),
  SUBSCRIBER_NUMBER(null, null),
  SELECTED_CHARACTER_SET(null, null),
  SELECTABLE_CHARACTER_SETS(null, null),
  SET_TEXT_MODE(null, null),
  CUSTOM_COMMAND(null, null)

  ;

  companion object {
    private const val DEFAULT_TIMEOUT_MS = 3000
  }

  constructor(timeout: Int?, requiredDelayFrom: ((other: SimModemCommandType) -> Int)?)
    : this(timeout ?: DEFAULT_TIMEOUT_MS, requiredDelayFrom ?: { 0 })
}