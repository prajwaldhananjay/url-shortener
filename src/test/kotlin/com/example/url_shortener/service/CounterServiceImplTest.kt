package com.example.url_shortener.service

import com.example.url_shortener.domain.Counter
import com.example.url_shortener.exception.CounterException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class CounterServiceImplTest {

    @Mock
    private lateinit var mongoTemplate: MongoTemplate

    private lateinit var counterService: CounterServiceImpl

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        counterService = CounterServiceImpl(mongoTemplate)
    }

    @Test
    fun `getNextSequence should return incremented value for successful counter update`() {
        val counterName = "testCounter"
        val initialValue = 1000L
        val expectedValue = 1001L
        val mockCounter = Counter(id = counterName, value = expectedValue)

        whenever(mongoTemplate.upsert(any<Query>(), any<Update>(), eq(Counter::class.java)))
            .thenReturn(null) // upsert result not used
        whenever(mongoTemplate.findAndModify(
            any<Query>(), 
            any<Update>(), 
            any<FindAndModifyOptions>(), 
            eq(Counter::class.java)
        )).thenReturn(mockCounter)

        val result = counterService.getNextSequence(counterName, initialValue)

        assertThat(result).isEqualTo(expectedValue)
        verify(mongoTemplate).upsert(any<Query>(), any<Update>(), eq(Counter::class.java))
        verify(mongoTemplate).findAndModify(
            any<Query>(), 
            any<Update>(), 
            any<FindAndModifyOptions>(), 
            eq(Counter::class.java)
        )
    }

    @Test
    fun `getNextSequence should throw CounterException when MongoDB findAndModify returns null`() {
        val counterName = "failingCounter"
        val initialValue = 1000L

        whenever(mongoTemplate.upsert(any<Query>(), any<Update>(), eq(Counter::class.java)))
            .thenReturn(null)
        whenever(mongoTemplate.findAndModify(
            any<Query>(), 
            any<Update>(), 
            any<FindAndModifyOptions>(), 
            eq(Counter::class.java)
        )).thenReturn(null)

        assertThatThrownBy {
            counterService.getNextSequence(counterName, initialValue)
        }.isInstanceOf(CounterException::class.java)
         .hasMessageContaining("Failed to retrieve or update sequence for: $counterName")

        verify(mongoTemplate).findAndModify(
            any<Query>(), 
            any<Update>(), 
            any<FindAndModifyOptions>(), 
            eq(Counter::class.java)
        )
    }

    @Test
    fun `getNextSequenceBatch should return correct range for valid batch size`() {
        val counterName = "batchCounter"
        val initialValue = 5000L
        val batchSize = 10
        val finalCounterValue = 5010L // After incrementing by batchSize
        val mockCounter = Counter(id = counterName, value = finalCounterValue)

        whenever(mongoTemplate.upsert(any<Query>(), any<Update>(), eq(Counter::class.java)))
            .thenReturn(null)
        whenever(mongoTemplate.findAndModify(
            any<Query>(), 
            any<Update>(), 
            any<FindAndModifyOptions>(), 
            eq(Counter::class.java)
        )).thenReturn(mockCounter)

        val result = counterService.getNextSequenceBatch(counterName, initialValue, batchSize)

        // Range should be 5001..5010 (startValue = endValue - batchSize + 1)
        val expectedStartValue = 5001L
        val expectedEndValue = 5010L
        assertThat(result.first).isEqualTo(expectedStartValue)
        assertThat(result.last).isEqualTo(expectedEndValue)
        assertThat(result.count()).isEqualTo(batchSize)
        
        verify(mongoTemplate).upsert(any<Query>(), any<Update>(), eq(Counter::class.java))
        verify(mongoTemplate).findAndModify(
            any<Query>(), 
            any<Update>(), 
            any<FindAndModifyOptions>(), 
            eq(Counter::class.java)
        )
    }

    @Test
    fun `getNextSequenceBatch should throw CounterException for invalid batch size`() {
        val counterName = "testCounter"
        val initialValue = 1000L
        val invalidBatchSizes = listOf(-1, 0)

        invalidBatchSizes.forEach { batchSize ->
            assertThatThrownBy {
                counterService.getNextSequenceBatch(counterName, initialValue, batchSize)
            }.isInstanceOf(CounterException::class.java)
             .hasMessageContaining("Batch size must be positive")
        }
    }

    @Test
    fun `getNextSequenceBatch should throw CounterException when MongoDB findAndModify returns null`() {
        val counterName = "failingBatchCounter"
        val initialValue = 1000L
        val batchSize = 5

        whenever(mongoTemplate.upsert(any<Query>(), any<Update>(), eq(Counter::class.java)))
            .thenReturn(null)
        whenever(mongoTemplate.findAndModify(
            any<Query>(), 
            any<Update>(), 
            any<FindAndModifyOptions>(), 
            eq(Counter::class.java)
        )).thenReturn(null)

        assertThatThrownBy {
            counterService.getNextSequenceBatch(counterName, initialValue, batchSize)
        }.isInstanceOf(CounterException::class.java)
         .hasMessageContaining("Failed to retrieve or update sequence for: $counterName")

        verify(mongoTemplate).findAndModify(
            any<Query>(), 
            any<Update>(), 
            any<FindAndModifyOptions>(), 
            eq(Counter::class.java)
        )
    }
}