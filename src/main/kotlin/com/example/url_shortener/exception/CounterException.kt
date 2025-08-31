package com.example.url_shortener.exception

class CounterException(message: String) : RuntimeException("Counter operation failed: $message")