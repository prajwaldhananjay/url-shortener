package com.example.url_shortener.service

import com.example.url_shortener.domain.ShortenedUrl
import com.example.url_shortener.domain.ShortenedUrlsRepository
import com.example.url_shortener.exception.ShortCodeNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class ShortCodeReadServiceImplTest {

    @Mock
    private lateinit var shortenedUrlsRepository: ShortenedUrlsRepository

    private lateinit var shortCodeReadService: ShortCodeReadServiceImpl

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        shortCodeReadService = ShortCodeReadServiceImpl(shortenedUrlsRepository)
    }

    @Test
    fun `getLongUrl should return original URL when provided with valid short code`() {
        val shortCode = "1l9ZoP6"
        val originalUrl = "https://example.com/very/long/path?param=value"
        val shortenedUrl = ShortenedUrl(
            id = "test-id",
            shortCode = shortCode,
            originalUrl = originalUrl,
            createdAt = Instant.now()
        )

        whenever(shortenedUrlsRepository.findByShortCode(shortCode)).thenReturn(shortenedUrl)

        val result = shortCodeReadService.getLongUrl(shortCode)

        assertThat(result).isEqualTo(originalUrl)
        verify(shortenedUrlsRepository).findByShortCode(shortCode)
    }

    @Test
    fun `getLongUrl should throw ShortCodeNotFoundException when short code does not exist`() {
        val nonExistentShortCode = "NOTFOUND"

        whenever(shortenedUrlsRepository.findByShortCode(nonExistentShortCode)).thenReturn(null)

        assertThatThrownBy {
            shortCodeReadService.getLongUrl(nonExistentShortCode)
        }.isInstanceOf(ShortCodeNotFoundException::class.java)
         .hasMessageContaining("Short code 'NOTFOUND' not found")

        verify(shortenedUrlsRepository).findByShortCode(nonExistentShortCode)
    }

    @Test
    fun `getLongUrl should handle empty short code gracefully`() {
        val emptyShortCode = ""

        whenever(shortenedUrlsRepository.findByShortCode(emptyShortCode)).thenReturn(null)

        assertThatThrownBy {
            shortCodeReadService.getLongUrl(emptyShortCode)
        }.isInstanceOf(ShortCodeNotFoundException::class.java)
         .hasMessageContaining("Short code '' not found")

        verify(shortenedUrlsRepository).findByShortCode(emptyShortCode)
    }

    @Test
    fun `getLongUrl should handle whitespace-only short code gracefully`() {
        val whitespaceShortCode = "   "

        whenever(shortenedUrlsRepository.findByShortCode(whitespaceShortCode)).thenReturn(null)

        assertThatThrownBy {
            shortCodeReadService.getLongUrl(whitespaceShortCode)
        }.isInstanceOf(ShortCodeNotFoundException::class.java)
         .hasMessageContaining("Short code '   ' not found")

        verify(shortenedUrlsRepository).findByShortCode(whitespaceShortCode)
    }

    @Test
    fun `getLongUrl should handle case-sensitive short codes correctly`() {
        val upperCaseShortCode = "ABC123D"
        val lowerCaseShortCode = "abc123d"
        val upperCaseUrl = "https://uppercase-example.com"
        val lowerCaseUrl = "https://lowercase-example.com"

        val upperCaseShortenedUrl = ShortenedUrl(
            id = "upper-id",
            shortCode = upperCaseShortCode,
            originalUrl = upperCaseUrl,
            createdAt = Instant.now()
        )

        val lowerCaseShortenedUrl = ShortenedUrl(
            id = "lower-id",
            shortCode = lowerCaseShortCode,
            originalUrl = lowerCaseUrl,
            createdAt = Instant.now()
        )

        whenever(shortenedUrlsRepository.findByShortCode(upperCaseShortCode)).thenReturn(upperCaseShortenedUrl)
        whenever(shortenedUrlsRepository.findByShortCode(lowerCaseShortCode)).thenReturn(lowerCaseShortenedUrl)

        val upperResult = shortCodeReadService.getLongUrl(upperCaseShortCode)
        val lowerResult = shortCodeReadService.getLongUrl(lowerCaseShortCode)

        assertThat(upperResult).isEqualTo(upperCaseUrl)
        assertThat(lowerResult).isEqualTo(lowerCaseUrl)
        assertThat(upperResult).isNotEqualTo(lowerResult)
    }
}