package com.sap.cdc.android.sdk.example.ui.view.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.sap.cdc.android.sdk.example.ui.theme.AppTheme
import com.sap.cdc.android.sdk.example.ui.view.flow.EmailRegisterView
import com.sap.cdc.android.sdk.example.ui.view.flow.EmailSignInView
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview
import kotlinx.coroutines.launch


/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Horizontal tab view pager for native UI authentication flows (register,sign in).
 */
@Composable
fun AuthenticationTabView(viewModel: IViewModelAuthentication, selected: Int) {
    val tabs = listOf("Register", "Sign In")

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { tabs.size }, initialPage = selected)
    val selectedTabIndex = remember { derivedStateOf { pagerState.currentPage } }

    Column(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxWidth()
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.White,
            indicator = { tabPositions ->
                // Indicator for the selected tab
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[ pagerState.currentPage]),
                    color = Color.Black
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title, style = AppTheme.typography.labelSmall) },
                    unselectedContentColor = Color.White,
                    selectedContentColor = Color.Black,
                    selected = selectedTabIndex.value == pagerState.currentPage,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()

        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (selectedTabIndex.value) {
                    0 -> EmailRegisterView(
                        viewModel = viewModel
                    )

                    1 -> EmailSignInView(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun AuthenticationTabViewPreview() {
    AppTheme {
        AuthenticationTabView(
            viewModel = ViewModelAuthenticationPreview(),
            selected = 0
        )
    }
}

