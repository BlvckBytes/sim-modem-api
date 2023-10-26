package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.ExecutionResult
import me.blvckbytes.simmodemapi.domain.SimModemCommand
import me.blvckbytes.simmodemapi.domain.SimModemCommandChain
import me.blvckbytes.simmodemapi.domain.SimModemResponse
import me.blvckbytes.simmodemapi.domain.port.SimModemSocketPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

@Component
class SimModemSocketAdapter(
  @Value("\${socket-server.host}")
  val socketServerHost: String,
  @Value("\${socket-server.port}")
  val socketServerPort: Int,
  @Value("\${socket-server.heartbeat-period-ms}")
  val socketServerHeartbeatPeriodMs: Long,
  @Value("\${socket-server.heartbeat-sentinel}")
  val socketServerHeartbeatSentinel: String
) : SimModemSocketPort {

  companion object {
    private const val READ_BUFFER_SIZE = 1024
    private const val NUMBER_OF_RECONNECT_TRIALS = 3
    private const val QUEUE_PEEK_DELAY_MS = 10L
    private const val COMMAND_HISTORY_SIZE = 32
  }

  private var socket: Socket? = null
  private var lastHeartbeatSend: Long = 0
  private val commandQueue = ConcurrentLinkedQueue<SimModemCommandChain>()
  private val logger = LoggerFactory.getLogger(SimModemSocketAdapter::class.java)

  private var commandTypeHistory = CommandTypeHistory(COMMAND_HISTORY_SIZE)

  init {
    startQueuePopThread()
    startHeartbeatThread()
  }

  override fun queueExecution(commandChain: SimModemCommandChain) {
    commandQueue.add(commandChain)
  }

  private fun startHeartbeatThread() {
    val sentinelBytes = socketServerHeartbeatSentinel.encodeToByteArray()

    thread(name="heartbeat") {
      while (true) {
        val millis = System.currentTimeMillis()

        if (millis - lastHeartbeatSend < socketServerHeartbeatPeriodMs) {
          Thread.sleep(socketServerHeartbeatPeriodMs / 2)
          continue
        }

        ensureAvailabilityAndExecute({}) { availableSocket ->
          val outputStream = availableSocket.getOutputStream()
          outputStream.write(sentinelBytes)
          outputStream.flush()
          lastHeartbeatSend = millis
        }
      }
    }
  }

  private fun startQueuePopThread() {
    thread(name = "queue-pop") {
      while (true) {
        var nextChain: SimModemCommandChain?

        synchronized(commandQueue) {
          nextChain = commandQueue.peek()

          if (nextChain == null)
            return@synchronized

          if (!commandTypeHistory.isReadyToBeExecuted(nextChain!!.type)) {
            nextChain = null
            return@synchronized
          }

          commandQueue.poll()
        }

        if (nextChain == null) {
          Thread.sleep(QUEUE_PEEK_DELAY_MS)
          continue
        }

        executeChain(nextChain!!)
        commandTypeHistory.add(nextChain!!.type)
      }
    }
  }

  private fun executeChain(commandChain: SimModemCommandChain) {
    val responses = mutableListOf<SimModemResponse>()

    for (command in commandChain.commands) {
      val result = executeCommand(command)

      if (result.first == ExecutionResult.UNAVAILABLE) {
        commandChain.resultHandler.handle(ExecutionResult.UNAVAILABLE, listOf())
        break
      }

      if (result.first == ExecutionResult.TIMED_OUT) {
        commandChain.resultHandler.handle(ExecutionResult.TIMED_OUT, listOf())
        break
      }

      responses.add(result.second!!)

      if (result.first != ExecutionResult.SUCCESS) {
        commandChain.resultHandler.handle(result.first, responses)
        break
      }
    }

    commandChain.resultHandler.handle(ExecutionResult.SUCCESS, responses)
  }

  private fun createConnection(): Boolean {
    if (socket != null)
      socket!!.close()

    return try {
      logger.info("Trying to connect to socket server on ${socketServerHost}:${socketServerPort}")
      socket = Socket(socketServerHost, socketServerPort)
      logger.info("Connected to socket server on ${socketServerHost}:${socketServerPort}")
      lastHeartbeatSend = 0
      true
    } catch (exception: java.lang.Exception) {
      logger.error("Could not connect to socket server on ${socketServerHost}:${socketServerPort}", exception.message)
      false
    }
  }

  private fun<T> ensureAvailabilityAndExecute(unavailabilityReturnSupplier: () -> T, executor: (availableSocket: Socket) -> T): T {
    if (socket == null) {
      for (i in 1..NUMBER_OF_RECONNECT_TRIALS) {
        if (createConnection())
          break

        if (i == NUMBER_OF_RECONNECT_TRIALS)
          return unavailabilityReturnSupplier()
      }
    }

    return try {
      executor.invoke(socket!!)
    } catch (exception: SocketException) {
      // TODO: Should there be a check against the exception message "Broken pipe"?
      // Force reconnect
      socket = null
      ensureAvailabilityAndExecute(unavailabilityReturnSupplier, executor)
    }
  }

  private fun executeCommand(command: SimModemCommand): Pair<ExecutionResult, SimModemResponse?> {
    return ensureAvailabilityAndExecute({ Pair(ExecutionResult.UNAVAILABLE, null) }) { availableSocket ->
      val outputStream = availableSocket.getOutputStream()
      val inputStream = availableSocket.getInputStream()

      // There could be remainders of previous responses still in the input buffer.
      // Also, sometimes, the modem seems to make requests on its own, which cause
      // responses that haven't even been requested by this process.
      try {
        availableSocket.soTimeout = 1
        while (inputStream.read() > 0)
          continue
      } catch (ignored: SocketTimeoutException) {}

      availableSocket.soTimeout = command.timeoutMs

      outputStream.write(command.binaryCommand)
      outputStream.flush()

      val commandSentStamp = LocalDateTime.now()

      try {
        val buffer = ByteArray(READ_BUFFER_SIZE)
        var amountRead = inputStream.read(buffer)
        var responseReceivedStamp = LocalDateTime.now()

        val responseBufferSlice = buffer.sliceArray(0 until amountRead)
        var responseContent = responseBufferSlice.decodeToString()

        // Man, what a HACK
        while (responseContent == "\r\n") {
          amountRead = inputStream.read(buffer)
          responseReceivedStamp = LocalDateTime.now()
          responseContent = buffer.decodeToString(0, amountRead)
        }

        val response = SimModemResponse(
          responseBufferSlice,
          CommandGeneratorAdapter.substituteUnprintableAscii(responseContent),
          command,
          commandSentStamp,
          responseReceivedStamp
        )

        if (command.responsePredicate?.apply(responseContent) == false)
          return@ensureAvailabilityAndExecute Pair(ExecutionResult.PREDICATE_MISMATCH, response)

        return@ensureAvailabilityAndExecute Pair(ExecutionResult.SUCCESS, response)

      } catch (exception: SocketTimeoutException) {
        return@ensureAvailabilityAndExecute Pair(ExecutionResult.TIMED_OUT, null)
      }
    }
  }
}