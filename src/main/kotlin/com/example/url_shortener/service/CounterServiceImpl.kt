package com.example.url_shortener.service

import com.example.url_shortener.domain.Counter
import com.example.url_shortener.exception.CounterException
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.stereotype.Service

@Service
class CounterServiceImpl(
    private val mongoTemplate: MongoTemplate
) : CounterService {
    
    override fun getNextSequence( counterName: String, initialValue: Long ): Long {
        val insertCounterInitialValue = Update().setOnInsert("value", initialValue)
        val query = Query(Criteria.where("name").`is`(counterName))
        mongoTemplate.upsert(query, insertCounterInitialValue, Counter::class.java)
        
        val increment = Update().inc("value", 1)
        val counter = mongoTemplate.findAndModify(
            query,
            increment,
            FindAndModifyOptions.options().returnNew(true),
            Counter::class.java
        )

        if ( counter == null ) {
            throw CounterException("Failed to retrieve or update sequence for: $counterName")
        }
        
        return counter.value
    }
    
    override fun getNextSequenceBatch( counterName: String, initialValue: Long, batchSize: Int ): LongRange {
        if (batchSize <= 0) {
            throw CounterException("Batch size must be positive")
        }
        
        val insertCounterInitialValue = Update().setOnInsert("value", initialValue)
        val query = Query(Criteria.where("name").`is`(counterName))
        mongoTemplate.upsert(query, insertCounterInitialValue, Counter::class.java)
        
        val increment = Update().inc("value", batchSize.toLong())
        val counter = mongoTemplate.findAndModify(
            query,
            increment,
            FindAndModifyOptions.options().returnNew(true),
            Counter::class.java
        )

        if ( counter == null ) {
            throw CounterException("Failed to retrieve or update sequence for: $counterName")
        }
        
        val endValue = counter.value
        val startValue = endValue - batchSize + 1
        
        return startValue..endValue
    }
}