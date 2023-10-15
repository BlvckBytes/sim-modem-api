package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.rest.SimModemSocketPort
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
    private const val READ_BUFFER_SIZE = 1024;
    private const val NUMBER_OF_RECONNECT_TRIALS = 3;
  }

  private var socket: Socket? = null
  private var lastHeartbeatSend: Long = 0
  private val commandQueue = ConcurrentLinkedQueue<Pair<List<SimModemCommand>, SimModemResultHandler>>()
  private val logger = LoggerFactory.getLogger(SimModemSocketAdapter::class.java)

  init {
    startQueuePopThread()
    startHeartbeatThread()
  }

  override fun queueExecution(commandChain: List<SimModemCommand>, resultHandler: SimModemResultHandler) {
    commandQueue.add(Pair(commandChain, resultHandler))
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
        val entry = commandQueue.poll()

        if (entry == null) {
          Thread.sleep(10)
          continue
        }

        executeChain(entry.first, entry.second)
      }
    }
  }

  private fun executeChain(commandChain: List<SimModemCommand>, resultHandler: SimModemResultHandler) {
    val responses = mutableListOf<SimModemResponse>()

    for (command in commandChain) {
      val result = executeCommand(command)

      if (result.first == ExecutionResult.UNAVAILABLE) {
        resultHandler.handle(ExecutionResult.UNAVAILABLE, listOf())
        break
      }

      if (result.first == ExecutionResult.TIMED_OUT) {
        resultHandler.handle(ExecutionResult.TIMED_OUT, listOf())
        break
      }

      responses.add(result.second!!)

      if (result.first != ExecutionResult.SUCCESS) {
        resultHandler.handle(result.first, responses)
        break
      }
    }

    resultHandler.handle(ExecutionResult.SUCCESS, responses)
  }

  private fun createConnection(): Boolean {
    if (socket != null)
      socket!!.close()

    return try {
      socket = Socket(socketServerHost, socketServerPort)
      logger.info("Connected to socket server on ${socketServerHost}:${socketServerPort}")
      lastHeartbeatSend = 0
      true;
    } catch (exception: java.lang.Exception) {
      logger.error("Could not connect to socket server on ${socketServerHost}:${socketServerPort}", exception)
      false;
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
      socket = null;
      ensureAvailabilityAndExecute(unavailabilityReturnSupplier, executor)
    }
  }

  private fun executeCommand(command: SimModemCommand): Pair<ExecutionResult, SimModemResponse?> {
    return ensureAvailabilityAndExecute({ Pair(ExecutionResult.UNAVAILABLE, null) }) { availableSocket ->
      val outputStream = availableSocket.getOutputStream()
      outputStream.write(command.command.encodeToByteArray())
      outputStream.flush()
      val commandSentStamp = LocalDateTime.now()

      availableSocket.soTimeout = command.timeoutMs

      val inputStream = availableSocket.getInputStream()

      try {
        val buffer = ByteArray(READ_BUFFER_SIZE)
        var amountRead = inputStream.read(buffer)
        var responseReceivedStamp = LocalDateTime.now()
        var responseContent = buffer.decodeToString(0, amountRead)

        // Man, what a HACK
        while (responseContent == "\r\n") {
          amountRead = inputStream.read(buffer)
          responseReceivedStamp = LocalDateTime.now()
          responseContent = buffer.decodeToString(0, amountRead)
        }

        val response = SimModemResponse(responseContent, command, commandSentStamp, responseReceivedStamp)

        if (command.responsePredicate?.apply(responseContent) == false)
          return@ensureAvailabilityAndExecute Pair(ExecutionResult.PREDICATE_MISMATCH, response)

        return@ensureAvailabilityAndExecute Pair(ExecutionResult.SUCCESS, response)

      } catch (exception: SocketTimeoutException) {
        return@ensureAvailabilityAndExecute Pair(ExecutionResult.TIMED_OUT, null)
      }
    }
  }
}