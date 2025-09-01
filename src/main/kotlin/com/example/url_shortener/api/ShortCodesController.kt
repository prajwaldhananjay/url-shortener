package com.example.url_shortener.api

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import java.net.URI
import com.example.url_shortener.data.ShortCodeResponse
import com.example.url_shortener.data.CreateShortCodeRequest
import com.example.url_shortener.service.ShortCodeReadService
import com.example.url_shortener.service.ShortCodeWriteService
import com.example.url_shortener.domain.ShortenedUrl
import com.example.url_shortener.config.UrlShortenerProperties
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Short Codes", description = "URL shortening operations")
class ShortCodesController(
    private val shortCodeReadService: ShortCodeReadService,
    private val shortCodeWriteService: ShortCodeWriteService,
    private val urlShortenerProperties: UrlShortenerProperties
) {

    @PostMapping("/short-codes")
    @Operation(
        summary = "Create a short code",
        description = "Creates a short code for the provided long URL"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Short code created successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ShortCodeResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid URL provided"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun createShortCode(@Valid @RequestBody request: CreateShortCodeRequest): ResponseEntity<ShortCodeResponse> {
        val shortenedUrlData = shortCodeWriteService.createShortCode(request.longUrl)
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ShortCodeResponse(urlShortenerProperties.baseUrl + shortenedUrlData.shortCode, shortenedUrlData.originalUrl, 
                shortenedUrlData.createdAt))
    }

    @GetMapping("/short-codes/{shortCode}")
    @Operation(
        summary = "Redirect to original URL",
        description = "Redirects to the original URL associated with the short code"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "301", description = "Redirect to original URL"),
            ApiResponse(responseCode = "404", description = "Short code not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun redirectToLongUrl(
        @Parameter(description = "The short code to redirect", required = true)
        @PathVariable shortCode: String
    ): ResponseEntity<Void> {
        val longUrl = shortCodeReadService.getLongUrl(shortCode)
        val headers = HttpHeaders()
        headers.location = URI.create(longUrl)
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).headers(headers).build()
    }

}
