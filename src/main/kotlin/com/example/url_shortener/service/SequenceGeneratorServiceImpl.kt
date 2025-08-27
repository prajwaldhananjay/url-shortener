package com.example.url_shortener.service

import com.example.url_shortener.domain.Counter
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.stereotype.Service

@Service
class SequenceGeneratorServiceImpl(
    private val mongoTemplate: MongoTemplate
) : SequenceGeneratorService {
    
    override fun generateSequence( counterName: String, initialValue: Long ): Long {
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
            throw RuntimeException("Failed to retrieve or update sequence for: $counterName")
        }
        
        return counter.value
    }
}