package com.example.url_shortener.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("URL Shortener API")
                    .description("A high-performance URL shortening service built with Spring Boot and Kotlin")
                    .version("v1.0")
                    .contact(
                        Contact()
                            .name("URL Shortener Team")
                            .url("https://github.com/prajwaldhananjay/url-shortener")
                    )
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("Development server"),
                    Server().url("https://qa-shortener.example.com").description("QA server"),
                    Server().url("https://myproject.de").description("Production server")
                )
            )
    }
}