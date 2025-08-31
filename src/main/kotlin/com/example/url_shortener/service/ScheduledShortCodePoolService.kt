package com.example.url_shortener.service

import com.example.url_shortener.config.ShortCodePoolProperties
import com.example.url_shortener.util.logger
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(
    prefix = "shortcode.pool", 
    name = ["enableScheduledGeneration"], 
    havingValue = "true", 
    matchIfMissing = true
)
class ScheduledShortCodePoolService(
    private val shortCodePoolService: ShortCodePoolService,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val shortCodePoolProperties: ShortCodePoolProperties
) {
    
    private val log = logger<ScheduledShortCodePoolService>()
    
    @Scheduled(
        initialDelayString = "\${shortcode.pool.initialDelayMs:60000}",
        fixedDelayString = "#{T(java.util.concurrent.TimeUnit).SECONDS.toMillis(\${shortcode.pool.schedulerFixedDelaySeconds:300})}"
    )
    fun replenishShortCodePool() {
        if (!shortCodePoolProperties.enableScheduledGeneration) {
            log.debug("Scheduled short code generation is disabled, skipping replenishment")
            return
        }
        
        try {
            if (acquireLock()) {
                try {
                    replenishPoolIfNeeded()
                } finally {
                    releaseLock()
                }
            } else {
                log.debug("Could not acquire lock for pool replenishment, skipping this run")
            }
        } catch (e: Exception) {
            log.error("Error during scheduled pool replenishment", e)
        }
    }
    
    private fun replenishPoolIfNeeded() {
        if (shortCodePoolService.needsReplenishment()) {
            val currentSize = shortCodePoolService.getPoolSize()
            val targetSize = shortCodePoolProperties.batchSize + shortCodePoolProperties.minPoolSize
            val neededCodes = maxOf(0, targetSize - currentSize.toInt())
            
            log.info("Pool needs replenishment. Current size: {}, Target size: {}, Generating: {}", 
                currentSize, targetSize, neededCodes)
            
            val generated = shortCodePoolService.generateAndStoreShortCodes(neededCodes)
            val finalSize = shortCodePoolService.getPoolSize()
            
            log.info("Pool replenishment completed. Generated: {}, Final pool size: {}", generated, finalSize)
        } else {
            val currentSize = shortCodePoolService.getPoolSize()
            log.debug("Pool size {} is above minimum threshold {}, no replenishment needed", 
                currentSize, shortCodePoolProperties.minPoolSize)
        }
    }
    
    private fun acquireLock(): Boolean {
        return try {
            val acquired = redisTemplate.opsForValue().setIfAbsent(
                shortCodePoolProperties.lockKey,
                System.currentTimeMillis().toString(),
                shortCodePoolProperties.lockTimeoutMs,
                TimeUnit.MILLISECONDS
            ) ?: false
            
            if (acquired) {
                log.debug("Acquired lock for pool replenishment")
            } else {
                log.debug("Failed to acquire lock for pool replenishment")
            }
            
            acquired
        } catch (e: Exception) {
            log.error("Error acquiring lock", e)
            false
        }
    }
    
    private fun releaseLock() {
        try {
            redisTemplate.delete(shortCodePoolProperties.lockKey)
            log.debug("Released lock for pool replenishment")
        } catch (e: Exception) {
            log.error("Error releasing lock", e)
        }
    }
    
    /**
     * Manually trigger pool replenishment (useful for testing or manual operations)
     */
    fun manualReplenishment() {
        log.info("Manual pool replenishment triggered")
        replenishShortCodePool()
    }
    
    /**
     * Get current pool statistics
     */
    fun getPoolStats(): Map<String, Any> {
        return mapOf(
            "currentSize" to shortCodePoolService.getPoolSize(),
            "minPoolSize" to shortCodePoolProperties.minPoolSize,
            "batchSize" to shortCodePoolProperties.batchSize,
            "needsReplenishment" to shortCodePoolService.needsReplenishment(),
            "scheduledGenerationEnabled" to shortCodePoolProperties.enableScheduledGeneration
        )
    }
}