package com.example.url_shortener.service

import org.springframework.stereotype.Service
import org.unbrokendome.base62.Base62
import java.net.URI
import com.example.url_shortener.domain.ShortenedUrlsRepository
import com.example.url_shortener.domain.ShortenedUrl
import com.example.url_shortener.exception.InvalidUrlException
import com.example.url_shortener.exception.ShortCodeGenerationException
import com.example.url_shortener.util.logger

@Service
class ShortCodeWriteServiceImpl(
    private val shortenedUrlsRepository: ShortenedUrlsRepository,
    private val counterService: CounterService,
    private val shortCodePoolService: ShortCodePoolService
) : ShortCodeWriteService {
    
    private val log = logger<ShortCodeWriteServiceImpl>()
    
    companion object {
        private val LOCALHOST_ADDRESSES = setOf("localhost", "127.0.0.1", "0.0.0.0")
        private val PRIVATE_IP_REGEX = Regex("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")
    }
    
    override fun createShortCode(longUrl: String): ShortenedUrl {
        validateUrl(longUrl)
        
        val shortenedUrl = shortenedUrlsRepository.findByOriginalUrl(longUrl)
        if (shortenedUrl != null) {
            log.debug("Existing short code found for URL: {}, returning: {}", longUrl, shortenedUrl.shortCode)
            return shortenedUrl
        }
        
        val shortCode = getOrGenerateShortCode()
        val shortenedUrlRecord = shortenedUrlsRepository.save(ShortenedUrl(shortCode = shortCode, originalUrl = longUrl, createdAt = java.time.Instant.now()))
        log.info("Successfully created short code: {} for URL: {}", shortCode, longUrl)
        return shortenedUrlRecord
    }
    
    private fun validateUrl(url: String) {
        try {
            val urlObj = URI(url).toURL()
            validateProtocol(urlObj.protocol)
            validateHost(urlObj.host)
        } catch (e: InvalidUrlException) {
            log.warn("URL validation failed for: {} - {}", url, e.message)
            throw e
        } catch (e: Exception) {
            log.warn("URL validation failed for: {} - malformed format", url)
            throw InvalidUrlException("Malformed URL format")
        }
    }
    
    private fun validateProtocol(protocol: String?) {
        if (protocol.isNullOrEmpty()) {
            log.warn("Protocol validation failed: empty protocol")
            throw InvalidUrlException("Protocol cannot be empty")
        }
        
        val normalizedProtocol = protocol.lowercase()
        if (normalizedProtocol !in setOf("http", "https")) {
            log.warn("Protocol validation failed: unsupported protocol: {}", protocol)
            throw InvalidUrlException("Only HTTP and HTTPS protocols are allowed")
        }
    }
    
    private fun validateHost(host: String?) {
        if (host.isNullOrEmpty()) {
            log.warn("Host validation failed: empty host")
            throw InvalidUrlException("Host cannot be empty")
        }
        
        val normalizedHost = host.lowercase()
        if (isPrivateOrLocalAddress(normalizedHost)) {
            log.warn("Host validation failed: private/local address rejected: {}", host)
            throw InvalidUrlException("Private/local network addresses are not allowed")
        }
    }
    
    private fun isPrivateOrLocalAddress(host: String): Boolean {
        return host in LOCALHOST_ADDRESSES ||
               host.startsWith("192.168.") ||
               host.startsWith("10.") ||
               host.matches(PRIVATE_IP_REGEX)
    }
    
    private fun getOrGenerateShortCode(): String {
        // Try to get a pre-generated short code from the pool first
        val pooledShortCode = shortCodePoolService.getAvailableShortCode()
        
        return if (pooledShortCode != null) {
            log.debug("Using pooled short code: {}", pooledShortCode)
            pooledShortCode
        } else {
            // Fallback to real-time generation
            log.debug("Pool empty, falling back to real-time generation")
            generateShortCode()
        }
    }
    
    private fun generateShortCode(): String {
        try {
            val counterValue = counterService.getNextSequence("shortCodeCounter", 100000000000L)
            val encoded = Base62.encode(counterValue)
            val shortCode = encoded.takeLast(7)
            log.debug("Generated short code via fallback: {}", shortCode)
            return shortCode
        } catch (e: Exception) {
            log.error("Failed to generate short code", e)
            throw ShortCodeGenerationException("Unable to generate short code: ${e.message}")
        }
    }
}