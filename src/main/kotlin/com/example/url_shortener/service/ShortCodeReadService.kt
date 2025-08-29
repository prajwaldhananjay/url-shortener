package com.example.url_shortener.service

interface ShortCodeReadService {
    fun getLongUrl(shortCode: String): String?
}