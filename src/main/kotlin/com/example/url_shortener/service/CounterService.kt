package com.example.url_shortener.service

interface CounterService {
    fun getNextSequence( counterName: String, initialValue: Long ): Long
    
    fun getNextSequenceBatch( counterName: String, initialValue: Long, batchSize: Int ): LongRange
    
}