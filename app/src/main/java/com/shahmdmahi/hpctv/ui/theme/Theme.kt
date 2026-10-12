package com.shahmdmahi.hpctv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

@OptIn(ExperimentalTvMaterial3Api::class)
private val HpcTvColorScheme = darkColorScheme(
    primary = HpcEmerald500,
    onPrimary = HpcTextWhite,
    primaryContainer = HpcEmeraldDark,
    onPrimaryContainer = HpcEmerald400,
    secondary = HpcAmber500,
    onSecondary = HpcTextWhite,
    secondaryContainer = HpcAmberDark,
    onSecondaryContainer = HpcAmber400,
    surface = HpcTvBackground,
    onSurface = HpcTextWhite,
    surfaceVariant = HpcCardBackground,
    onSurfaceVariant = HpcTextZinc300,
    border = HpcBorderZinc800
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HPCTVTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = HpcTvColorScheme,
        typography = Typography,
        content = content
    )
}