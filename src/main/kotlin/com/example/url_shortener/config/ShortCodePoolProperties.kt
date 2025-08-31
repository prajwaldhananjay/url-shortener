package com.example.url_shortener.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "shortcode.pool")
data class ShortCodePoolProperties(
    var batchSize: Int = 10,
    var minPoolSize: Int = 5,
    var schedulerFixedDelaySeconds: Long = 300L, // 5 minutes between runs
    var initialDelayMs: Long = 60000L, // Wait 1 minute after app startup before first run
    var cacheKeyPrefix: String = "shortcode:pool",
    var lockKey: String = "shortcode:pool:lock",
    var lockTimeoutMs: Long = 30000L, // 30 seconds
    var enableScheduledGeneration: Boolean = true
)