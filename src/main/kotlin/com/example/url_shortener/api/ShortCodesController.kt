package com.example.url_shortener.api

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
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

}
