package com.example.url_shortener.data

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to create a short code for a URL")
data class CreateShortCodeRequest(
    @field:NotBlank(message = "URL must not be blank")
    @field:Pattern(
        regexp = "^https?://.*", 
        message = "URL must start with http:// or https://"
    )
    @Schema(description = "The long URL to be shortened", example = "https://www.example.com/very/long/path")
    val longUrl: String
)