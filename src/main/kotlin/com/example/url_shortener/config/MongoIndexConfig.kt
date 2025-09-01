package com.example.url_shortener.config

import com.example.url_shortener.domain.ShortenedUrl
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.IndexOperations
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.stereotype.Component

@Component
class MongoIndexConfig(
    private val mongoTemplate: MongoTemplate
) {
    private val logger = LoggerFactory.getLogger(MongoIndexConfig::class.java)

    @EventListener(ContextRefreshedEvent::class)
    fun initIndicesAfterStartup() {
        try {
            createIndexesForDocumentEntities()
            logger.info("MongoDB indexes initialized")
        } catch (e: Exception) {
            logger.error("Error creating indexes", e)
        }
    }

    private fun createIndexesForDocumentEntities() {
        val mappingContext = mongoTemplate.converter.mappingContext
        
        mappingContext.persistentEntities
            .filter { it.isAnnotationPresent(Document::class.java) }
            .forEach { entity ->
                createIndexesForEntity(entity.type)
            }
    }

    private fun createIndexesForEntity(entityType: Class<*>) {
        when (entityType) {
            ShortenedUrl::class.java -> createShortenedUrlIndexes()
        }
    }

    private fun createShortenedUrlIndexes() {
        val indexOps = mongoTemplate.indexOps(ShortenedUrl::class.java)
        val existingIndexNames = getExistingIndexNames(indexOps)
        
        createIndexIfNotExists(indexOps, existingIndexNames, SHORT_CODE_INDEX_NAME) {
            buildShortCodeIndex()
        }
        
        createIndexIfNotExists(indexOps, existingIndexNames, ORIGINAL_URL_INDEX_NAME) {
            buildOriginalUrlIndex()
        }
    }

    private fun getExistingIndexNames(indexOps: IndexOperations): Set<String> {
        return indexOps.indexInfo.mapNotNull { it.name }.toSet()
    }

    private fun createIndexIfNotExists(
        indexOps: IndexOperations,
        existingIndexNames: Set<String>,
        indexName: String,
        indexBuilder: () -> Index
    ) {
        if (indexName !in existingIndexNames) {
            logger.info("Created index: $indexName")
            indexOps.createIndex(indexBuilder())
        }
    }

    private fun buildShortCodeIndex(): Index {
        return Index()
            .on("shortCode", Sort.Direction.ASC)
            .unique()
            .named(SHORT_CODE_INDEX_NAME)
    }

    private fun buildOriginalUrlIndex(): Index {
        return Index()
            .on("originalUrl", Sort.Direction.ASC)
            .unique()
            .named(ORIGINAL_URL_INDEX_NAME)
    }

    companion object {
        private const val SHORT_CODE_INDEX_NAME = "idx_short_code"
        private const val ORIGINAL_URL_INDEX_NAME = "idx_original_url"
    }
}