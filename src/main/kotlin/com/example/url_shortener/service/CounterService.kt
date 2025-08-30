package com.example.url_shortener.service

interface CounterService {
    fun getNextSequence( counterName: String, initialValue: Long ): Long
    
}