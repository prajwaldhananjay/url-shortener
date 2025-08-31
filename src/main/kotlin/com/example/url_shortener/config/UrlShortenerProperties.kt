package com.example.url_shortener.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "url-shortener")
data class UrlShortenerProperties(
    var baseUrl: String = "http://localhost:8080/"
)