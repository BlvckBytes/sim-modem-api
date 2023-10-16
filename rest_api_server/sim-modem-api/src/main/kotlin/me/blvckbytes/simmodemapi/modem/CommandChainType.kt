package me.blvckbytes.simmodemapi.modem

enum class CommandChainType(
  val requiredDelayFrom: (other: CommandChainType?) -> Int
) {
  // SMS messages seem to get stuck for a long time when sent in bursts
  SEND_SMS({ if (it == SEND_SMS) 3500 else 0 }),
  SIGNAL_QUALITY({ 0 }),
  SELECTED_CHARACTER_SET({ 0 }),
  SELECTABLE_CHARACTER_SETS({ 0 }),
  CUSTOM_COMMAND({ 0 })
}