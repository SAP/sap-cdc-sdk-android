package com.sap.cdc.android.sdk.example.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.R

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

@Preview
@Composable
fun ViewHome() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.size(20.dp))
        Text("The all-new", fontSize = 20.sp, fontWeight = FontWeight.Light)
        Text("MacBook Pro", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text("with Retina display", fontSize = 16.sp, fontWeight = FontWeight.Normal)
        Spacer(modifier = Modifier.size(20.dp))
        OutlinedButton(
            modifier = Modifier.size(width = 240.dp, height = 44.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                //TODO: Click buy now...
            }) {
            Text("Buy Now")
        }
        Image(
            modifier = Modifier.size(width = 200.dp, height = 200.dp),
            painter = painterResource(id = R.drawable.img_home_macbook),
            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
        )
        Row(
            Modifier.padding(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
                modifier = Modifier.size(width = 150.dp, height = 200.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(width = 150.dp, height = 120.dp),
                    ) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            painter = painterResource(id = R.drawable.img_home_great_sounds),
                            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Great Sounds", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "Listening Experiences",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

            }
            Spacer(modifier = Modifier.size(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
                modifier = Modifier
                    .size(width = 150.dp, height = 200.dp)
                    .background(color = Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(width = 150.dp, height = 120.dp),
                    ) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            painter = painterResource(id = R.drawable.img_home_essentials),
                            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Essentials", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "All you need",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

            }
        }
        Row(
            Modifier.padding(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
                modifier = Modifier.size(width = 150.dp, height = 200.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(width = 150.dp, height = 120.dp),
                    ) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            painter = painterResource(id = R.drawable.img_home_vr),
                            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Virtual Reality", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "Dive into the details",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

            }
            Spacer(modifier = Modifier.size(10.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
                modifier = Modifier
                    .size(width = 150.dp, height = 200.dp)
                    .background(color = Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(width = 150.dp, height = 120.dp),
                    ) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            painter = painterResource(id = R.drawable.img_home_performance),
                            contentDescription = stringResource(id = R.string.home_img_macbook_content_description)
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Text("Performance", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        "Powerful devices",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

            }
        }
    }
}