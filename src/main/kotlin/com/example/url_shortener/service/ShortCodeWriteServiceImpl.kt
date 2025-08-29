package com.example.url_shortener.service

import org.springframework.stereotype.Service
import org.unbrokendome.base62.Base62
import java.net.URI
import com.example.url_shortener.domain.ShortenedUrlsRepository
import com.example.url_shortener.domain.ShortenedUrl

@Service
class ShortCodeWriteServiceImpl(
    private val shortenedUrlsRepository: ShortenedUrlsRepository,
    private val sequenceGeneratorService: SequenceGeneratorService
) : ShortCodeWriteService {
    
    companion object {
        private val ALLOWED_PROTOCOLS = setOf("http", "https")
        private val LOCALHOST_ADDRESSES = setOf("localhost", "127.0.0.1", "0.0.0.0")
        private val PRIVATE_IP_REGEX = Regex("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")
    }
    
    override fun createShortCode(longUrl: String): ShortenedUrl {
        validateUrl(longUrl)
        
        val shortenedUrl = shortenedUrlsRepository.findByOriginalUrl(longUrl)
        if (shortenedUrl != null) {
            return shortenedUrl
        }
        
        val shortCode = generateShortCode()
        val shortendUrlRecord = shortenedUrlsRepository.save(ShortenedUrl(shortCode = shortCode, originalUrl = longUrl, createdAt = java.time.Instant.now()))
        println("Save operation complete. Saved short code: ${shortCode}")
        return shortendUrlRecord
    }
    
    private fun validateUrl(url: String) {
        try {
            val urlObj = URI(url).toURL()
            validateProtocol(urlObj.protocol)
            validateHost(urlObj.host)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid URL: Malformed URL format")
        }
    }
    
    private fun validateProtocol(protocol: String) {
        if (protocol.lowercase() !in ALLOWED_PROTOCOLS) {
            throw IllegalArgumentException("Invalid URL: Only HTTP and HTTPS protocols are allowed")
        }
    }
    
    private fun validateHost(host: String?) {
        if (host.isNullOrEmpty()) {
            throw IllegalArgumentException("Invalid URL: Host cannot be empty")
        }
        
        val normalizedHost = host.lowercase()
        if (isPrivateOrLocalAddress(normalizedHost)) {
            throw IllegalArgumentException("Invalid URL: Private/local network addresses are not allowed")
        }
    }
    
    private fun isPrivateOrLocalAddress(host: String): Boolean {
        return host in LOCALHOST_ADDRESSES ||
               host.startsWith("192.168.") ||
               host.startsWith("10.") ||
               host.matches(PRIVATE_IP_REGEX)
    }
    
    private fun generateShortCode(): String {
        val counterValue = sequenceGeneratorService.generateSequence("shortCodeCounter", 100000000000L)
        val encoded = Base62.encode(counterValue)
        return encoded.takeLast(7)
    }
}