package com.example.url_shortener.api

import com.example.url_shortener.data.CreateShortCodeRequest
import com.example.url_shortener.domain.ShortenedUrl
import com.example.url_shortener.exception.InvalidUrlException
import com.example.url_shortener.exception.ShortCodeGenerationException
import com.example.url_shortener.exception.ShortCodeNotFoundException
import com.example.url_shortener.service.ShortCodeReadService
import com.example.url_shortener.service.ShortCodeWriteService
import com.example.url_shortener.config.UrlShortenerProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant

@WebMvcTest(ShortCodesController::class)
class ShortCodesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var shortCodeReadService: ShortCodeReadService

    @MockBean
    private lateinit var shortCodeWriteService: ShortCodeWriteService

    @MockBean
    private lateinit var urlShortenerProperties: UrlShortenerProperties

    @Test
    fun `POST short-codes should return 201 Created with valid request`() {
        val request = CreateShortCodeRequest("https://example.com/very-long-url")
        val shortenedUrl = ShortenedUrl(
            id = "test-id",
            shortCode = "ABC123D",
            originalUrl = "https://example.com/very-long-url",
            createdAt = Instant.parse("2024-01-01T00:00:00Z")
        )

        whenever(shortCodeWriteService.createShortCode(request.longUrl)).thenReturn(shortenedUrl)
        whenever(urlShortenerProperties.baseUrl).thenReturn("https://myproject.de/")

        mockMvc.perform(
            post("/api/v1/short-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated)
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.shortUrl").value("https://myproject.de/ABC123D"))
        .andExpect(jsonPath("$.longUrl").value("https://example.com/very-long-url"))
        .andExpect(jsonPath("$.createdAt").value("2024-01-01T00:00:00Z"))

        verify(shortCodeWriteService).createShortCode(request.longUrl)
    }

    @Test
    fun `POST short-codes should return 400 Bad Request for invalid URL format`() {
        val invalidRequest = CreateShortCodeRequest("not-a-valid-url")

        mockMvc.perform(
            post("/api/v1/short-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
        .andExpect(status().isBadRequest)
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed for the request payload."))
        .andExpect(jsonPath("$.errors.longUrl").value("URL must start with http:// or https://"))
    }

    @Test
    fun `POST short-codes should return 400 Bad Request for blank URL`() {
        val blankRequest = CreateShortCodeRequest("")

        mockMvc.perform(
            post("/api/v1/short-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blankRequest))
        )
        .andExpect(status().isBadRequest)
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed for the request payload."))
        .andExpect(jsonPath("$.errors.longUrl").exists())
    }

    @Test
    fun `POST short-codes should return 400 Bad Request when InvalidUrlException is thrown`() {
        val request = CreateShortCodeRequest("http://localhost:8080")

        whenever(shortCodeWriteService.createShortCode(any()))
            .thenThrow(InvalidUrlException("Private/local network addresses are not allowed"))

        mockMvc.perform(
            post("/api/v1/short-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest)
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Invalid URL: Private/local network addresses are not allowed"))
    }

    @Test
    fun `POST short-codes should return 500 Internal Server Error when ShortCodeGenerationException is thrown`() {
        val request = CreateShortCodeRequest("https://example.com")

        whenever(shortCodeWriteService.createShortCode(any()))
            .thenThrow(ShortCodeGenerationException("Unable to generate short code: Counter service failed"))

        mockMvc.perform(
            post("/api/v1/short-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isInternalServerError)
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.error").value("Internal Server Error"))
        .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `GET shortCode should return 301 Moved Permanently with Location header for valid short code`() {
        val shortCode = "1l9ZoA4"
        val originalUrl = "https://example.com/very-long-url"

        whenever(shortCodeReadService.getLongUrl(shortCode)).thenReturn(originalUrl)

        mockMvc.perform(get("/api/v1/short-codes/{shortCode}", shortCode))
            .andExpect(status().isMovedPermanently)
            .andExpect(header().string("Location", originalUrl))
            .andExpect(content().string(""))

        verify(shortCodeReadService).getLongUrl(shortCode)
    }

    @Test
    fun `GET shortCode should return 404 Not Found for non-existent short code`() {
        val shortCode = "NOTFOUND"

        whenever(shortCodeReadService.getLongUrl(shortCode))
            .thenThrow(ShortCodeNotFoundException(shortCode))

        mockMvc.perform(get("/api/v1/short-codes/{shortCode}", shortCode))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Short code 'NOTFOUND' not found"))

        verify(shortCodeReadService).getLongUrl(shortCode)
    }


    @Test
    fun `POST short-codes should return 415 Unsupported Media Type for non-JSON content`() {
        mockMvc.perform(
            post("/api/v1/short-codes")
                .contentType(MediaType.TEXT_PLAIN)
                .content("https://example.com")
        )
        .andExpect(status().isUnsupportedMediaType)
    }

}