package com.sap.cdc.android.sdk.core

/**
 * Test implementation of ResourceProvider for unit testing.
 * Allows injecting mock resource values without requiring Android Context.
 */
class TestResourceProvider : ResourceProvider {
    private val resources = mutableMapOf<String, String>()
    
    /**
     * Adds a resource value for testing.
     * @param key The resource key
     * @param value The resource value
     * @return This instance for method chaining
     */
    fun addResource(key: String, value: String): TestResourceProvider = apply {
        resources[key] = value
    }
    
    override fun getRequiredString(key: String): String {
        return resources[key] ?: throw IllegalArgumentException("Resource $key not found")
    }
    
    override fun getString(key: String): String? {
        return resources[key]
    }
    
    /**
     * Clears all resources.
     */
    fun clear() {
        resources.clear()
    }
}
