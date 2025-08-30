package com.example.url_shortener.data

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class CreateShortCodeRequest(
    @field:NotBlank(message = "URL must not be blank")
    @field:Pattern(
        regexp = "^https?://.*", 
        message = "URL must start with http:// or https://"
    )
    val longUrl: String
)