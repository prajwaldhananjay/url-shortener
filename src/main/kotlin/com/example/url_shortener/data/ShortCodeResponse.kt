package com.example.url_shortener.data

import java.time.Instant
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response containing the created short code")
data class ShortCodeResponse(
    @Schema(description = "The shortened URL", example = "https://myproject.de/1l9ZwsG")
    val shortUrl: String,
    @Schema(description = "The original long URL", example = "https://www.example.com/very/long/path")
    val longUrl: String,
    @Schema(description = "Timestamp when the short code was created")
    val createdAt: Instant
)