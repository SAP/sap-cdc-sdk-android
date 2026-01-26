// SPDX-FileCopyrightText: Copyright (c) 2024 SAP SE or an SAP affiliate company and sap-cdc-sdk-android contributors
// SPDX-License-Identifier: Apache-2.0

package com.sap.cdc.bitsnbytes.feature.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sap.cdc.bitsnbytes.apptheme.AppTheme

/**
 * Activity to display parsed MRZ data.
 */
class MRZResultActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_DOCUMENT_TYPE = "document_type"
        const val EXTRA_COUNTRY_CODE = "country_code"
        const val EXTRA_SURNAME = "surname"
        const val EXTRA_GIVEN_NAMES = "given_names"
        const val EXTRA_DOCUMENT_NUMBER = "document_number"
        const val EXTRA_NATIONALITY = "nationality"
        const val EXTRA_DATE_OF_BIRTH = "date_of_birth"
        const val EXTRA_SEX = "sex"
        const val EXTRA_EXPIRATION_DATE = "expiration_date"
        const val EXTRA_PERSONAL_NUMBER = "personal_number"
        const val EXTRA_IS_VALID = "is_valid"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Extract MRZ data from intent
        val documentType = intent.getStringExtra(EXTRA_DOCUMENT_TYPE) ?: "Unknown"
        val countryCode = intent.getStringExtra(EXTRA_COUNTRY_CODE) ?: ""
        val surname = intent.getStringExtra(EXTRA_SURNAME) ?: ""
        val givenNames = intent.getStringExtra(EXTRA_GIVEN_NAMES) ?: ""
        val documentNumber = intent.getStringExtra(EXTRA_DOCUMENT_NUMBER) ?: ""
        val nationality = intent.getStringExtra(EXTRA_NATIONALITY) ?: ""
        val dateOfBirth = intent.getStringExtra(EXTRA_DATE_OF_BIRTH) ?: ""
        val sex = intent.getStringExtra(EXTRA_SEX) ?: ""
        val expirationDate = intent.getStringExtra(EXTRA_EXPIRATION_DATE) ?: ""
        val personalNumber = intent.getStringExtra(EXTRA_PERSONAL_NUMBER)
        val isValid = intent.getBooleanExtra(EXTRA_IS_VALID, false)
        
        setContent {
            AppTheme {
                MRZResultScreen(
                    documentType = documentType,
                    countryCode = countryCode,
                    surname = surname,
                    givenNames = givenNames,
                    documentNumber = documentNumber,
                    nationality = nationality,
                    dateOfBirth = dateOfBirth,
                    sex = sex,
                    expirationDate = expirationDate,
                    personalNumber = personalNumber,
                    isValid = isValid,
                    onClose = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MRZResultScreen(
    documentType: String,
    countryCode: String,
    surname: String,
    givenNames: String,
    documentNumber: String,
    nationality: String,
    dateOfBirth: String,
    sex: String,
    expirationDate: String,
    personalNumber: String?,
    isValid: Boolean,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MRZ Scan Result") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Validation status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isValid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isValid) "✅ Valid Document" else "❌ Invalid Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isValid) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Document information
            DataField("Document Type", documentType)
            DataField("Country Code", countryCode)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            DataField("Surname", surname)
            DataField("Given Names", givenNames)
            DataField("Full Name", "$givenNames $surname".trim())
            DataField("Sex", sex)
            DataField("Date of Birth", dateOfBirth)
            DataField("Nationality", nationality)
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Document Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            DataField("Document Number", documentNumber)
            DataField("Expiration Date", expirationDate)
            if (personalNumber != null) {
                DataField("Personal Number", personalNumber)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Close button
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun DataField(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value.ifEmpty { "—" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
