package com.sap.cdc.android.sdk.sceensets.extensions

import java.util.Locale


/**
 * Created by Tal Mirmelshtein on 21/06/2024
 * Copyright: SAP LTD.
 */

fun String.capitalFirst(): String = this.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(
        Locale.getDefault()
    ) else it.toString()
}