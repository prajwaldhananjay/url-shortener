package com.example.url_shortener.globalexception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.HttpMediaTypeNotSupportedException
import java.time.Instant
import com.example.url_shortener.exception.ShortCodeNotFoundException
import com.example.url_shortener.exception.InvalidUrlException
import com.example.url_shortener.exception.ShortCodeGenerationException
import com.example.url_shortener.exception.ErrorResponse
import com.example.url_shortener.util.logger


@ControllerAdvice
class GlobalExceptionHandler {
    
    private val log = logger<GlobalExceptionHandler>()

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "Invalid value")
        }
        
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Validation failed for the request payload.",
            path = request.getDescription(false).removePrefix("uri="),
            errors = errors
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(ShortCodeNotFoundException::class)
    fun handleShortCodeNotFound(
        ex: ShortCodeNotFoundException, 
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Short code not found",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(InvalidUrlException::class)
    fun handleInvalidUrl(
        ex: InvalidUrlException, 
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid URL provided",
            path = request.getDescription(false).removePrefix("uri=")
        )
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(ShortCodeGenerationException::class)
    fun handleShortCodeGeneration(
        ex: ShortCodeGenerationException, 
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "Failed to generate short code",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(
        ex: HttpMediaTypeNotSupportedException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            error = "Unsupported Media Type",
            message = ex.message ?: "Content type not supported",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception, 
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception occurred", ex)

        val errorResponse = ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}