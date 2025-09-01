package com.example.url_shortener.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "shortened_urls")
data class ShortenedUrl(
    @Id
    val id: String? = null,
    @Indexed(unique = true, name = "idx_short_code")
    val shortCode: String,
    @Indexed(name = "idx_original_url")
    val originalUrl: String,
    val createdAt: Instant
)