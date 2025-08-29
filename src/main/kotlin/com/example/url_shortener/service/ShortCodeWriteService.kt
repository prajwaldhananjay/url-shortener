package com.example.url_shortener.service

import com.example.url_shortener.domain.ShortenedUrl

interface ShortCodeWriteService {
    fun createShortCode(longUrl: String): ShortenedUrl
}