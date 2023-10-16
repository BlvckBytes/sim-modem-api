package me.blvckbytes.simmodemapi.modem

enum class CommandChainType(
  val requiredDelayFrom: (other: CommandChainType?) -> Int
) {
  // SMS messages seem to get stuck for a long time when sent in bursts
  SEND_SMS({ if (it == SEND_SMS) 3500 else 0 }),
  HEALTH({ 0 }),
}