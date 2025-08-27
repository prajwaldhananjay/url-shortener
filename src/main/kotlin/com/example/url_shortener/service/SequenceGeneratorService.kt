package com.example.url_shortener.service

interface SequenceGeneratorService {
    fun generateSequence( counterName: String, initialValue: Long ): Long
    
}