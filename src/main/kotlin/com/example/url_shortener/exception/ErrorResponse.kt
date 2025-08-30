package com.example.url_shortener.exception

import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val errors: Map<String, String>? = null
)