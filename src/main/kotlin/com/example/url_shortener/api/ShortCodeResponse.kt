package com.example.url_shortener.api

import java.time.Instant

data class ShortCodeResponse(
    val shortUrl: String,
    val longUrl: String,
    val createdAt: Instant
)