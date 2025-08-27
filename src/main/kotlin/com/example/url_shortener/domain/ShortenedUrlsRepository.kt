package com.example.url_shortener.domain

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import com.example.url_shortener.domain.ShortenedUrl

@Repository
interface ShortenedUrlsRepository : MongoRepository<ShortenedUrl, String> {

    fun findByShortCode(shortCode: String): ShortenedUrl?

    fun findByOriginalUrl(originalUrl: String): ShortenedUrl?
}