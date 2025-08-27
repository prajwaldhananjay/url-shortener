package com.example.url_shortener.service

import com.example.url_shortener.domain.ShortenedUrl

interface ShortCodeService {

    fun createShortCode(longUrl: String): ShortenedUrl  // returns full short URL string
}
