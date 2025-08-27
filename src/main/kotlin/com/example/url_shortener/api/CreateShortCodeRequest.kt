package com.example.url_shortener.api

import jakarta.validation.constraints.NotBlank

data class CreateShortCodeRequest(
    @field:NotBlank(message = "longUrl must not be blank")
    val longUrl: String
)