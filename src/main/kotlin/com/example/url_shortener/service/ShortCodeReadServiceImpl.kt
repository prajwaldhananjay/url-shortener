package com.example.url_shortener.service

import org.springframework.stereotype.Service
import org.springframework.cache.annotation.Cacheable
import com.example.url_shortener.domain.ShortenedUrlsRepository
import com.example.url_shortener.service.ShortCodeReadService

@Service
class ShortCodeReadServiceImpl(
    private val shortenedUrlsRepository: ShortenedUrlsRepository
) : ShortCodeReadService {

    @Cacheable(value = ["shortCodes"], key = "#shortCode")
    override fun getLongUrl(shortCode: String): String? {
        val shortenedUrl = shortenedUrlsRepository.findByShortCode(shortCode)
        return shortenedUrl?.originalUrl
    }
}