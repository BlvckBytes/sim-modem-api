package me.blvckbytes.simmodemapi.rest.config

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime

class ApiError(
  val status: HttpStatus,
  val message: String,
  val subErrors: List<ApiSubError> = listOf(),
  @field:JsonFormat(
    shape = JsonFormat.Shape.STRING,
    pattern = "dd-MM-yyyy hh:mm:ss"
  )
  val timestamp: LocalDateTime = LocalDateTime.now(),
) {

  fun toResponseEntity(): ResponseEntity<Any> {
    return ResponseEntity(this, status)
  }
}