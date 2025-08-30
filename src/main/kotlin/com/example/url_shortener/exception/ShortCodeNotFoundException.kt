package com.example.url_shortener.exception

class ShortCodeNotFoundException(shortCode: String) : RuntimeException("Short code '$shortCode' not found")