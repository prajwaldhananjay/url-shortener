package com.example.url_shortener

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class UrlShortenerApplication

fun main(args: Array<String>) {
	runApplication<UrlShortenerApplication>(*args)
}
