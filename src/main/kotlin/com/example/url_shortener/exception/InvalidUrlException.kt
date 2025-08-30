package com.example.url_shortener.exception

class InvalidUrlException(message: String) : RuntimeException("Invalid URL: $message")