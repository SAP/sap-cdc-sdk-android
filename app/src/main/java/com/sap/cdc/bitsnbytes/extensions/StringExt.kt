package com.sap.cdc.bitsnbytes.extensions


/**
 * Helper extension method to split first/last name.
 */
fun String.splitFullName(): Pair<String?, String?> {
    val names = this.trim().split(Regex("\\s+"))
    return names.firstOrNull() to names.lastOrNull()
}