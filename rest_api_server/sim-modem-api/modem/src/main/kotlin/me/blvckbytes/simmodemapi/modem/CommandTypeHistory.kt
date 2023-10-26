package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.CommandChainType

class CommandTypeHistory(
  private val size: Int
) {

  private val lastCommands: Array<Pair<CommandChainType, Long>?> = arrayOfNulls(size)
  private var nextCommandIndex = 0

  fun add(type: CommandChainType) {
    val index = nextCommandIndex++

    if (nextCommandIndex == size)
      nextCommandIndex = 0

    lastCommands[index] = Pair(type, System.currentTimeMillis())
  }

  fun isReadyToBeExecuted(type: CommandChainType): Boolean {
    val millis = System.currentTimeMillis()
    for (i in 0 until size) {
      var index = nextCommandIndex - i - 1

      if (index < 0)
        index += size

      val lastCommand = lastCommands[index] ?: break

      if (millis - lastCommand.second < type.requiredDelayFrom(lastCommand.first))
        return false
    }
    return true
  }
}