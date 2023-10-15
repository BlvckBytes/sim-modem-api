package me.blvckbytes.simmodemapi.rest.config

import org.springframework.beans.TypeMismatchException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class RestExceptionHandler : ResponseEntityExceptionHandler() {

  override fun handleAsyncRequestTimeoutException(
    ex: AsyncRequestTimeoutException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest
  ): ResponseEntity<Any>? {
    return ApiError(
      HttpStatus.SERVICE_UNAVAILABLE,
      "The request could not be served within the timeout period and had to be abandoned"
    ).toResponseEntity()
  }

  override fun handleMethodArgumentNotValid(
    exception: MethodArgumentNotValidException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest
  ): ResponseEntity<Any>? {
    val subErrors = mutableListOf<ApiSubError>()

    exception.bindingResult.allErrors.forEach { error ->
      if (error !is FieldError)
        return@forEach

      subErrors.add(ApiFieldValidationError(
        error.field,
        error.rejectedValue,
        error.defaultMessage ?: "Description not yet implemented"
      ))
    }

    return ApiError(
      HttpStatus.BAD_REQUEST,
      "The provided data did not pass validation",
      subErrors
    ).toResponseEntity()
  }

  override fun handleTypeMismatch(
    exception: TypeMismatchException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest
  ): ResponseEntity<Any>? {
    return ApiError(
      HttpStatus.NOT_FOUND,
      "Could not convert ${exception.value} into a ${exception.requiredType?.simpleName ?: "?"}${
        exception.requiredType?.enumConstants?.joinToString(", ", " enum (", ")") ?: ""
      } for field ${exception.propertyName}"
    ).toResponseEntity()
  }

  override fun handleMissingServletRequestPart(
    exception: MissingServletRequestPartException,
    headers: HttpHeaders,
    status: HttpStatusCode,
    request: WebRequest
  ): ResponseEntity<Any>? {
    return ApiError(
      HttpStatus.BAD_REQUEST,
      "The required multipart/form-data key '${exception.requestPartName}' is absent"
    ).toResponseEntity()
  }

  @ExceptionHandler(MultipartException::class)
  fun handleMultipartException(exception: MultipartException): ResponseEntity<Any> {
    return ApiError(
      HttpStatus.BAD_REQUEST,
      "Could not parse the multipart/form-data request (missing upload?)"
    ).toResponseEntity()
  }

  @ExceptionHandler(DescribedInternalException::class)
  fun handleDescribedInternalException(exception: DescribedInternalException): ResponseEntity<Any> {
    return ApiError(
      HttpStatus.INTERNAL_SERVER_ERROR,
      exception.message ?: "There has been no message provided"
    ).toResponseEntity()
  }
}