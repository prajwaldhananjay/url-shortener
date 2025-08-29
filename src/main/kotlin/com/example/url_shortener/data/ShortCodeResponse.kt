package com.example.url_shortener.data

import java.time.Instant

data class ShortCodeResponse(
    val shortUrl: String,
    val longUrl: String,
    val createdAt: Instant
)