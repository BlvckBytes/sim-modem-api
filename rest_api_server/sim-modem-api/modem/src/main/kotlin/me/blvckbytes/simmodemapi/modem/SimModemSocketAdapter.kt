package me.blvckbytes.simmodemapi.modem

import me.blvckbytes.simmodemapi.domain.ExecutionResult
import me.blvckbytes.simmodemapi.domain.SimModemCommand
import me.blvckbytes.simmodemapi.domain.SimModemCommandChain
import me.blvckbytes.simmodemapi.domain.SimModemResponse
import me.blvckbytes.simmodemapi.domain.port.SimModemSocketPort
import me.blvckbytes.simmodemapi.domain.textcoder.ASCIITextCoder
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
  private val readBuffer = ByteArray(READ_BUFFER_SIZE)
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
        val nextChain = commandQueue.poll()

        if (nextChain == null) {
          ensureAvailabilityAndExecute({}, this::handleModemMessages)
          Thread.sleep(QUEUE_PEEK_DELAY_MS)
          continue
        }

        executeChain(nextChain)
      }
    }
  }

  private fun executeChain(commandChain: SimModemCommandChain) {
    val responses = mutableListOf<SimModemResponse>()

    for (command in commandChain.commands) {

      val remainingRequiredDelay = commandTypeHistory.getRemainingRequiredDelay(command.type)

      if (remainingRequiredDelay > 0)
        Thread.sleep(remainingRequiredDelay)

      val result = executeCommand(command)
      commandTypeHistory.add(command.type)

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
      // Force reconnect
      socket = null
      ensureAvailabilityAndExecute(unavailabilityReturnSupplier, executor)
    }
  }

  private fun tryReadSocketInputStream(socket: Socket, timeout: Int): Pair<ByteArray, String>? {
    socket.soTimeout = timeout

    try {
      val inputStream = socket.getInputStream()

      var responseBufferSlice: ByteArray? = null
      var responseContent: String? = null

      // Some commands produce leading \r\n sequences which are sent apart from the
      // actual response and thus cause a separate read() trigger. These sequences have
      // no value and need to be discarded, to get to the data of interest.
      while (responseContent == null || responseContent == "\r\n") {
        val amountRead = inputStream.read(readBuffer)

        if (amountRead < 0)
          return null

        responseBufferSlice = readBuffer.sliceArray(0 until amountRead)
        responseContent = responseBufferSlice.decodeToString()
      }

      return Pair(responseBufferSlice!!, responseContent)
    } catch (exception: SocketTimeoutException) {
      return null
    }
  }

  private fun handleModemMessages(socket: Socket) {
    val (_, responseString) = tryReadSocketInputStream(socket, 1) ?: return
    val trimmedResponseString = ASCIITextCoder.trimControlCharacters(responseString)
    val firstColonIndex = trimmedResponseString.indexOf(':')

    if (firstColonIndex < 0) {
      logger.info("Discarding unknown modem message: $trimmedResponseString")
      return
    }

    val responseCommandPrefix = trimmedResponseString.substring(0, firstColonIndex)

    if (responseCommandPrefix == "+CMT") {
      // TODO: Actually handle the message
      logger.info("Received modem message '+CMT': $trimmedResponseString")
      return
    }

    logger.info("Discarding modem message with no handler: $trimmedResponseString")
  }

  private fun executeCommand(command: SimModemCommand): Pair<ExecutionResult, SimModemResponse?> {
    return ensureAvailabilityAndExecute({ Pair(ExecutionResult.UNAVAILABLE, null) }) { availableSocket ->
      val outputStream = availableSocket.getOutputStream()

      handleModemMessages(availableSocket)

      outputStream.write(command.binaryCommand)
      outputStream.flush()

      val commandSentStamp = LocalDateTime.now()
      val responseTimeout = command.customTimeoutMs ?: command.type.timeoutMs

      val (responseBytes, responseString) = tryReadSocketInputStream(availableSocket, responseTimeout)
        ?: return@ensureAvailabilityAndExecute Pair(ExecutionResult.TIMED_OUT, null)

      val responseReceivedStamp = LocalDateTime.now()

      val response = SimModemResponse(
        responseBytes,
        ASCIITextCoder.substituteUnprintableAscii(responseString),
        command,
        commandSentStamp,
        responseReceivedStamp
      )

      if (command.responsePredicate?.apply(responseString) == false)
        return@ensureAvailabilityAndExecute Pair(ExecutionResult.PREDICATE_MISMATCH, response)

      return@ensureAvailabilityAndExecute Pair(ExecutionResult.SUCCESS, response)
    }
  }
}