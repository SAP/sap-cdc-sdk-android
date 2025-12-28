package com.sap.cdc.bitsnbytes.extensions


/**
 * Helper extension method to split first/last name.
 */
fun String.splitFullName(): Pair<String?, String?> {
    val names = this.trim().split(Regex("\\s+"))
    return names.firstOrNull() to names.lastOrNull()
}

/**
 * Helper extension method to parse String value for required missing fields list.
 */
fun String.parseRequiredMissingFieldsForRegistration(): List<String> {
    val fields = this.substringAfterLast(": ")
    return fields.split(", ")
}