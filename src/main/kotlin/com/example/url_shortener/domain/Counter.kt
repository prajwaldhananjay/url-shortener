package com.example.url_shortener.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "counters")
data class Counter(
    @Id
    val id: String,
    val value: Long
)