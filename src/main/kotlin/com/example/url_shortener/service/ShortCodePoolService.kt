package com.example.url_shortener.service

interface ShortCodePoolService {
    
    /**
     * Gets an available short code from the pool
     * @return A short code if available, null if pool is empty
     */
    fun getAvailableShortCode(): String?
    
    /**
     * Gets the current count of available short codes in the pool
     * @return The count of available short codes
     */
    fun getPoolSize(): Long
    
    /**
     * Generates and adds a batch of short codes to the pool
     * @param batchSize Number of short codes to generate
     * @return Number of short codes successfully added
     */
    fun generateAndStoreShortCodes(batchSize: Int): Int
    
    /**
     * Checks if the pool needs replenishment based on minimum threshold
     * @return True if pool size is below minimum threshold
     */
    fun needsReplenishment(): Boolean
    
    /**
     * Removes all short codes from the pool (for testing/maintenance)
     */
    fun clearPool()
}