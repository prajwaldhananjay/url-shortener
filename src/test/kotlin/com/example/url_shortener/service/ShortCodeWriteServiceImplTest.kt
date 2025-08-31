package com.example.url_shortener.service

import com.example.url_shortener.domain.ShortenedUrl
import com.example.url_shortener.domain.ShortenedUrlsRepository
import com.example.url_shortener.exception.InvalidUrlException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.lang.reflect.Method
import java.time.Instant

class ShortCodeWriteServiceImplTest {

    @Mock
    private lateinit var shortenedUrlsRepository: ShortenedUrlsRepository

    @Mock
    private lateinit var counterService: CounterService

    @Mock
    private lateinit var shortCodePoolService: ShortCodePoolService

    private lateinit var shortCodeWriteService: ShortCodeWriteServiceImpl

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        shortCodeWriteService = ShortCodeWriteServiceImpl(
            shortenedUrlsRepository,
            counterService,
            shortCodePoolService
        )
    }

    @Test
    fun `createShortCode should return unique shortCodes of length 7 & valid base62 chars when provided with unique URLs`() {
        var counterValue = 100000000000L
        val generatedCodes = mutableSetOf<String>()
        val testUrl = "https://example.com"

        whenever(shortCodePoolService.getAvailableShortCode()).thenReturn(null)
        whenever(shortenedUrlsRepository.findByOriginalUrl(any())).thenReturn(null)
        whenever(shortenedUrlsRepository.save(any<ShortenedUrl>())).thenAnswer { invocation ->
            val input = invocation.getArgument(0) as ShortenedUrl
            input.copy(id = "mock-id-${System.currentTimeMillis()}")
        }
        whenever(counterService.getNextSequence("shortCodeCounter", 100000000000L)).thenAnswer {
            counterValue++
        }

        repeat(100) {
            val result = shortCodeWriteService.createShortCode(testUrl)
            generatedCodes.add(result.shortCode)
        }

        // Assert that all codes are of length 7, unique, and valid Base62 characters
        generatedCodes.forEach { code ->
            assertThat(code).hasSize(7)
        }

        assertThat(generatedCodes).hasSize(100)
        
        val validChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        generatedCodes.forEach { code ->
            code.toCharArray().forEach { char ->
                assertThat(validChars).contains(char.toString())
            }
        }
    }

    @Test
    fun `createShortCode should use pooled code when available`() {
        val pooledShortCode = "ABC123D"
        val testUrl = "https://example.com"

        whenever(shortCodePoolService.getAvailableShortCode()).thenReturn(pooledShortCode)
        whenever(shortenedUrlsRepository.findByOriginalUrl(testUrl)).thenReturn(null)
        whenever(shortenedUrlsRepository.save(any<ShortenedUrl>())).thenAnswer { invocation ->
            val input = invocation.getArgument(0) as ShortenedUrl
            input.copy(id = "mock-id-${System.currentTimeMillis()}")
        }

        val result = shortCodeWriteService.createShortCode(testUrl)

        //Assert that the pooled code was used
        assertThat(result.shortCode).isEqualTo(pooledShortCode)
    }

    @Test
    fun `createShortCode should generate code when pool is empty`() {
        val testUrl = "https://example.com"
        var counterValue = 100000000000L

        whenever(shortCodePoolService.getAvailableShortCode()).thenReturn(null)
        whenever(shortenedUrlsRepository.findByOriginalUrl(testUrl)).thenReturn(null)
        whenever(shortenedUrlsRepository.save(any<ShortenedUrl>())).thenAnswer { invocation ->
            val input = invocation.getArgument(0) as ShortenedUrl
            input.copy(id = "mock-id-${System.currentTimeMillis()}")
        }
        whenever(counterService.getNextSequence("shortCodeCounter", 100000000000L)).thenAnswer {
            ++counterValue
        }

        val result = shortCodeWriteService.createShortCode(testUrl)

        // Assert that a generated code was used
        assertThat(result.shortCode).hasSize(7)
        val validChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        result.shortCode.toCharArray().forEach { char ->
            assertThat(validChars).contains(char.toString())
        }
    }

    @Test
    fun `createShortCode should return the same code for a pre-existing URL`() {
        val testUrl = "https://example.com"
        val existingShortCode = "1l9ZoA7"
        val existingShortenedUrl = ShortenedUrl(
            id = "existing-id",
            shortCode = existingShortCode,
            originalUrl = testUrl,
            createdAt = Instant.now()
        )

        whenever(shortenedUrlsRepository.findByOriginalUrl(testUrl)).thenReturn(existingShortenedUrl)

        val result = shortCodeWriteService.createShortCode(testUrl)

        assertThat(result.shortCode).isEqualTo(existingShortCode)
        assertThat(result.originalUrl).isEqualTo(testUrl)
        assertThat(result.id).isEqualTo("existing-id")
    }

    @Test
    fun `validateUrl should accept valid URLs when provided with HTTP and HTTPS URLs`() {
        val validUrls = listOf(
            "https://example.com",
            "https://www.google.com",
            "https://subdomain.example.org/path",
            "http://example.com",
            "http://www.example.org/path",
            "http://api.service.com:3000/endpoint"
        )

        whenever(shortCodePoolService.getAvailableShortCode()).thenReturn("TEST123")
        whenever(shortenedUrlsRepository.findByOriginalUrl(any())).thenReturn(null)
        whenever(shortenedUrlsRepository.save(any<ShortenedUrl>())).thenAnswer { invocation ->
            val input = invocation.getArgument(0) as ShortenedUrl
            input.copy(id = "mock-id")
        }

        validUrls.forEach { url ->
            val result = shortCodeWriteService.createShortCode(url)
            assertThat(result.originalUrl).isEqualTo(url)
        }
    }

    @Test
    fun `validateUrl should throw InvalidUrlException when provided with malformed URLs`() {
        val invalidUrls = listOf(
            "not-a-url",
            "http://",
            "https://",
            "",
            "   ",
            "://example.com",
            "htp://example.com"
        )

        invalidUrls.forEach { url ->
            assertThatThrownBy {
                shortCodeWriteService.createShortCode(url)
            }.isInstanceOf(InvalidUrlException::class.java)
             .hasMessageContaining("Invalid URL")
        }
    }

    @Test
    fun `validateUrl should throw InvalidUrlException when provided with unsupported protocols`() {
        val unsupportedProtocolUrls = listOf(
            "ftp://example.com",
            "file:///etc/passwd",
            "ws://example.com",
            "wss://example.com",
            "javascript:alert('xss')",
            "data:text/html,<script>alert('xss')</script>",
            "mailto:test@example.com"
        )

        unsupportedProtocolUrls.forEach { url ->
            assertThatThrownBy {
                shortCodeWriteService.createShortCode(url)
            }.isInstanceOf(InvalidUrlException::class.java)
             .hasMessageMatching(".*Invalid URL.*|.*Only HTTP and HTTPS protocols are allowed.*")
        }
    }

    @Test
    fun `validateUrl should throw InvalidUrlException when provided with localhost addresses`() {
        val localhostUrls = listOf(
            "http://localhost",
            "https://localhost:8080",
            "http://127.0.0.1",
            "https://127.0.0.1:3000/path",
            "http://0.0.0.0",
            "https://0.0.0.0:8080"
        )

        localhostUrls.forEach { url ->
            assertThatThrownBy {
                shortCodeWriteService.createShortCode(url)
            }.isInstanceOf(InvalidUrlException::class.java)
             .hasMessageContaining("Private/local network addresses are not allowed")
        }
    }

    @Test
    fun `validateUrl should throw InvalidUrlException when provided with private IP addresses`() {
        val privateIpUrls = listOf(
            "http://192.168.1.1",
            "https://192.168.0.100:8080",
            "http://10.0.0.1",
            "https://10.1.1.1/path",
            "http://172.16.0.1",
            "https://172.31.255.255:3000"
        )

        privateIpUrls.forEach { url ->
            assertThatThrownBy {
                shortCodeWriteService.createShortCode(url)
            }.isInstanceOf(InvalidUrlException::class.java)
             .hasMessageContaining("Private/local network addresses are not allowed")
        }
    }

    @Test
    fun `validateUrl should throw InvalidUrlException when provided with URLs having empty host`() {
        assertThatThrownBy {
            shortCodeWriteService.createShortCode("http:///path")
        }.isInstanceOf(InvalidUrlException::class.java)
         .hasMessageContaining("Host cannot be empty")
    }

    @Test
    fun `createShortCode should return same shortCode for identical URLs called multiple times`() {
        val testUrl = "https://example.com/path?param=value"
        val mockShortCode = "1l9ZoA7"

        whenever(shortCodePoolService.getAvailableShortCode()).thenReturn(mockShortCode)
        whenever(shortenedUrlsRepository.findByOriginalUrl(testUrl)).thenReturn(null)
        whenever(shortenedUrlsRepository.save(any<ShortenedUrl>())).thenAnswer { invocation ->
            val input = invocation.getArgument(0) as ShortenedUrl
            input.copy(id = "mock-id")
        }

        val firstResult = shortCodeWriteService.createShortCode(testUrl)
        
        // Mock that the URL now exists in database
        whenever(shortenedUrlsRepository.findByOriginalUrl(testUrl)).thenReturn(firstResult)
        
        val secondResult = shortCodeWriteService.createShortCode(testUrl)
        val thirdResult = shortCodeWriteService.createShortCode(testUrl)

        assertThat(firstResult.shortCode).isEqualTo(mockShortCode)
        assertThat(secondResult.shortCode).isEqualTo(firstResult.shortCode)
        assertThat(thirdResult.shortCode).isEqualTo(firstResult.shortCode)
        assertThat(secondResult.originalUrl).isEqualTo(testUrl)
        assertThat(thirdResult.originalUrl).isEqualTo(testUrl)
    }

}