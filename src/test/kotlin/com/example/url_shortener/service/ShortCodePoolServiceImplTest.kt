package com.example.url_shortener.service

import com.example.url_shortener.config.ShortCodePoolProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations

class ShortCodePoolServiceImplTest {

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    @Mock
    private lateinit var setOperations: SetOperations<String, Any>

    @Mock
    private lateinit var counterService: CounterService

    @Mock
    private lateinit var shortCodePoolProperties: ShortCodePoolProperties

    private lateinit var shortCodePoolService: ShortCodePoolServiceImpl

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(redisTemplate.opsForSet()).thenReturn(setOperations)
        whenever(shortCodePoolProperties.cacheKeyPrefix).thenReturn("shortcode:pool")
        whenever(shortCodePoolProperties.minPoolSize).thenReturn(5)
        
        shortCodePoolService = ShortCodePoolServiceImpl(
            redisTemplate,
            counterService,
            shortCodePoolProperties
        )
    }

    @Test
    fun `getAvailableShortCode should return short code when pool has available codes`() {
        val expectedShortCode = "1l9ZoA7"
        whenever(setOperations.pop("shortcode:pool:codes")).thenReturn(expectedShortCode)

        val result = shortCodePoolService.getAvailableShortCode()

        assertThat(result).isEqualTo(expectedShortCode)
        verify(setOperations).pop("shortcode:pool:codes")
    }

    @Test
    fun `getAvailableShortCode should return null when pool is empty`() {
        whenever(setOperations.pop("shortcode:pool:codes")).thenReturn(null)

        val result = shortCodePoolService.getAvailableShortCode()

        assertThat(result).isNull()
        verify(setOperations).pop("shortcode:pool:codes")
    }

    @Test
    fun `generateAndStoreShortCodes should generate and store valid batch of short codes`() {
        val batchSize = 10
        val counterRange = 100000000000L..100000000009L
        whenever(counterService.getNextSequenceBatch("shortCodeCounter", 100000000000L, batchSize))
            .thenReturn(counterRange)
        whenever(setOperations.add(eq("shortcode:pool:codes"), anyVararg())).thenReturn(10L)

        val result = shortCodePoolService.generateAndStoreShortCodes(batchSize)

        assertThat(result).isEqualTo(10)
        verify(counterService).getNextSequenceBatch("shortCodeCounter", 100000000000L, batchSize)
        verify(setOperations).add(eq("shortcode:pool:codes"), anyVararg())
    }

    @Test
    fun `getPoolSize should return correct pool size when Redis is accessible`() {
        val expectedSize = 25L
        whenever(setOperations.size("shortcode:pool:codes")).thenReturn(expectedSize)

        val result = shortCodePoolService.getPoolSize()

        assertThat(result).isEqualTo(expectedSize)
        verify(setOperations).size("shortcode:pool:codes")
    }

    @Test
    fun `needsReplenishment should return true when pool size is below minimum threshold`() {
        val currentPoolSize = 3L
        val minPoolSize = 5
        whenever(setOperations.size("shortcode:pool:codes")).thenReturn(currentPoolSize)
        whenever(shortCodePoolProperties.minPoolSize).thenReturn(minPoolSize)

        val result = shortCodePoolService.needsReplenishment()

        assertThat(result).isTrue()
        verify(setOperations).size("shortcode:pool:codes")
    }
}