package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.SimModemCommandType
import kotlin.math.max

class CommandTypeHistory(
  private val size: Int
) {

  private val lastCommands: Array<Pair<SimModemCommandType, Long>?> = arrayOfNulls(size)
  private var nextCommandIndex = 0

  fun add(type: SimModemCommandType) {
    val index = nextCommandIndex++

    if (nextCommandIndex == size)
      nextCommandIndex = 0

    lastCommands[index] = Pair(type, System.currentTimeMillis())
  }

  fun getRemainingRequiredDelay(type: SimModemCommandType): Long {
    val millis = System.currentTimeMillis()
    var maxRemainingRequiredDelay = 0L

    for (i in 0 until size) {
      var index = nextCommandIndex - i - 1

      if (index < 0)
        index += size

      val (commandType, executionMillis) = lastCommands[index] ?: break

      val elapsedTime = millis - executionMillis
      val requiredDelay = type.requiredDelayFrom(commandType)

      if (elapsedTime < requiredDelay) {
        val remainingRequiredDelay = requiredDelay - elapsedTime

        if (remainingRequiredDelay > maxRemainingRequiredDelay)
          maxRemainingRequiredDelay = remainingRequiredDelay
      }
    }

    return maxRemainingRequiredDelay
  }
}