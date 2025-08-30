package com.example.url_shortener.exception

class ShortCodeGenerationException(message: String) : RuntimeException("Failed to generate short code: $message")