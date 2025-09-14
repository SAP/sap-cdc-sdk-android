package com.sap.cdc.bitsnbytes.ui.view.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sap.cdc.bitsnbytes.ui.view.model.Country
import com.sap.cdc.bitsnbytes.ui.view.model.CountryData

/**
 * Country code selector component that displays the selected country flag and code
 * and allows users to select a different country from a dialog
 */
@Composable
fun CountryCodeSelector(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    // Country selector button
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Flag emoji
            Text(
                text = selectedCountry.flagEmoji,
                fontSize = 20.sp
            )
            
            // Country code
            Text(
                text = selectedCountry.dialCode,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            
            // Dropdown arrow
            Text(
                text = "▼",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }

    // Country selection dialog
    if (showDialog) {
        CountrySelectionDialog(
            onCountrySelected = { country ->
                onCountrySelected(country)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Dialog for selecting a country from the list
 */
@Composable
private fun CountrySelectionDialog(
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter countries based on search query
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            CountryData.countries
        } else {
            CountryData.countries.filter { country ->
                country.name.contains(searchQuery, ignoreCase = true) ||
                country.dialCode.contains(searchQuery) ||
                country.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Select Country",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search field
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search countries...",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Countries list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCountries) { country ->
                        CountryItem(
                            country = country,
                            onClick = { onCountrySelected(country) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual country item in the selection list
 */
@Composable
private fun CountryItem(
    country: Country,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .clip(RoundedCornerShape(8.dp)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Flag emoji
            Text(
                text = country.flagEmoji,
                fontSize = 24.sp
            )
            
            // Country name and dial code
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = country.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = country.dialCode,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview
@Composable
private fun CountryCodeSelectorPreview() {
    val defaultCountry = CountryData.getDefaultCountry()
    CountryCodeSelector(
        selectedCountry = defaultCountry,
        onCountrySelected = { }
    )
}

@Preview
@Composable
private fun CountryItemPreview() {
    CountryItem(
        country = Country("US", "+1", "United States", "🇺🇸"),
        onClick = { }
    )
}
