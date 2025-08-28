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
import com.example.url_shortener.api.ShortCodeResponse
import com.example.url_shortener.api.CreateShortCodeRequest
import com.example.url_shortener.service.ShortCodeService
import com.example.url_shortener.domain.ShortenedUrl
import jakarta.validation.Valid

@RestController
@RequestMapping("/api/v1")
class ShortCodesController(private val shortCodeService: ShortCodeService) {

    private val baseUrl = "https://myproject.de/"

    @PostMapping("/create-short-code")
    fun createShortCode(@Valid @RequestBody request: CreateShortCodeRequest): ResponseEntity<ShortCodeResponse> {
        val shortenedUrlData = shortCodeService.createShortCode(request.longUrl)
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ShortCodeResponse(baseUrl + shortenedUrlData.shortCode, shortenedUrlData.originalUrl, 
                shortenedUrlData.createdAt))
    }

    @GetMapping("/{shortCode}")
    fun redirectToLongUrl(@PathVariable shortCode: String): ResponseEntity<Void> {
        val longUrl = shortCodeService.getLongUrl(shortCode)
        
        return if (longUrl != null) {
            val headers = HttpHeaders()
            headers.location = URI.create(longUrl)
            ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).headers(headers).build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

}
