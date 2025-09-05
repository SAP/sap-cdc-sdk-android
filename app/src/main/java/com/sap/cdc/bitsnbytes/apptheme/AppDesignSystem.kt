package com.sap.cdc.bitsnbytes.apptheme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Created by Tal Mirmelshtein on 14/12/2024
 * Copyright: SAP LTD.
 */

@Immutable
data class AppColorScheme(
    val background: Color,
    val primary: Color,
    val secondary: Color,
)

@Immutable
data class AppTypography(
    val topBar: TextStyle,
    val titleLarge: TextStyle,
    val titleNormal: TextStyle,
    val titleSmall: TextStyle,
    val titleNormalLight: TextStyle,
    val body: TextStyle,
    val labelLarge: TextStyle,
    val labelNormal: TextStyle,
    val labelSmall: TextStyle,
)

@Immutable
data class AppSize(
    val small: Dp,
    val medium: Dp,
    val normal: Dp,
    val large: Dp,
    val spacerMedium: Dp,
    val spacerSmall: Dp,
    val spacerLarge: Dp,
)

val LocalAppColorScheme = staticCompositionLocalOf {
    AppColorScheme(
        background = Color.Unspecified,
        primary = Color.Unspecified,
        secondary = Color.Unspecified,
    )
}

val LocalAppTypography = staticCompositionLocalOf {
    AppTypography(
        topBar = TextStyle.Default,
        titleLarge = TextStyle.Default,
        titleNormal = TextStyle.Default,
        titleSmall = TextStyle.Default,
        titleNormalLight = TextStyle.Default,
        body = TextStyle.Default,
        labelLarge = TextStyle.Default,
        labelSmall = TextStyle.Default,
        labelNormal = TextStyle.Default,
    )
}

val LocalAppSize = staticCompositionLocalOf {
    AppSize(
        small = Dp.Unspecified,
        medium = Dp.Unspecified,
        normal = Dp.Unspecified,
        large = Dp.Unspecified,
        spacerMedium = Dp.Unspecified,
        spacerSmall = Dp.Unspecified,
        spacerLarge = Dp.Unspecified,
    )
}