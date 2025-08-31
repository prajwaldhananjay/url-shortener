package com.example.url_shortener.service

import com.example.url_shortener.config.ShortCodePoolProperties
import com.example.url_shortener.util.logger
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import org.unbrokendome.base62.Base62
import java.util.concurrent.TimeUnit

@Service
class ShortCodePoolServiceImpl(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val counterService: CounterService,
    private val shortCodePoolProperties: ShortCodePoolProperties
) : ShortCodePoolService {
    
    private val log = logger<ShortCodePoolServiceImpl>()
    
    companion object {
        private const val POOL_KEY_SUFFIX = ":codes"
        private const val COUNTER_NAME = "shortCodeCounter"
        private const val COUNTER_START_VALUE = 100000000000L
    }
    
    private val poolKey: String
        get() = "${shortCodePoolProperties.cacheKeyPrefix}$POOL_KEY_SUFFIX"
    
    override fun getAvailableShortCode(): String? {
        return try {
            val shortCode = redisTemplate.opsForSet().pop(poolKey) as? String
            if (shortCode == null) {
                log.warn("Pool is empty, no short codes available")
            }
            shortCode
        } catch (e: Exception) {
            log.error("Error retrieving short code from pool", e)
            null
        }
    }
    
    override fun getPoolSize(): Long {
        return try {
            redisTemplate.opsForSet().size(poolKey) ?: 0L
        } catch (e: Exception) {
            log.error("Error getting pool size", e)
            0L
        }
    }
    
    override fun generateAndStoreShortCodes(batchSize: Int): Int {
        if (batchSize <= 0) {
            log.warn("Invalid batch size: {}", batchSize)
            return 0
        }
        
        return try {
            val counterRange = counterService.getNextSequenceBatch(COUNTER_NAME, COUNTER_START_VALUE, batchSize)
            val shortCodes = counterRange.map { counterValue ->
                val encoded = Base62.encode(counterValue)
                encoded.takeLast(7)
            }.toTypedArray()
            
            val addedCount = redisTemplate.opsForSet().add(poolKey, *shortCodes)?.toInt() ?: 0
            log.info("Generated and stored {} short codes in pool using batch counter (attempted: {})", addedCount, batchSize)
            return addedCount
        } catch (e: Exception) {
            log.error("Error generating and storing short codes", e)
            0
        }
    }
    
    override fun needsReplenishment(): Boolean {
        val currentSize = getPoolSize()
        val needsReplenishment = currentSize < shortCodePoolProperties.minPoolSize
        
        if (needsReplenishment) {
            log.debug("Pool needs replenishment. Current size: {}, Min size: {}", 
                currentSize, shortCodePoolProperties.minPoolSize)
        }
        
        return needsReplenishment
    }
    
    override fun clearPool() {
        try {
            redisTemplate.delete(poolKey)
            log.info("Cleared short code pool")
        } catch (e: Exception) {
            log.error("Error clearing pool", e)
        }
    }
}