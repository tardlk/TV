package com.github.tvbox.osc.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import com.github.tvbox.osc.R

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTheme(content: @Composable () -> Unit) {
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = base.copy(
            primary = colorResource(R.color.tv_accent),
            onPrimary = colorResource(R.color.tv_on_accent),
            primaryContainer = colorResource(R.color.tv_selected),
            onPrimaryContainer = colorResource(R.color.tv_text_primary),
            secondary = colorResource(R.color.tv_accent),
            onSecondary = colorResource(R.color.tv_on_accent),
            secondaryContainer = colorResource(R.color.tv_surface_high),
            onSecondaryContainer = colorResource(R.color.tv_text_primary),
            tertiary = colorResource(R.color.tv_accent),
            onTertiary = colorResource(R.color.tv_on_accent),
            tertiaryContainer = colorResource(R.color.tv_selected),
            onTertiaryContainer = colorResource(R.color.tv_text_primary),
            background = colorResource(R.color.tv_background),
            onBackground = colorResource(R.color.tv_text_primary),
            surface = colorResource(R.color.tv_surface),
            onSurface = colorResource(R.color.tv_text_primary),
            surfaceVariant = colorResource(R.color.tv_surface_high),
            onSurfaceVariant = colorResource(R.color.tv_text_secondary),
            surfaceTint = colorResource(R.color.tv_accent),
            inverseSurface = colorResource(R.color.tv_text_primary),
            inverseOnSurface = colorResource(R.color.tv_background),
            border = colorResource(R.color.tv_outline),
            borderVariant = colorResource(R.color.tv_line),
        ),
        typography = Typography(
            displaySmall = tvStyle(44f, 52f, FontWeight.Bold),
            headlineLarge = tvStyle(36f, 44f, FontWeight.Bold),
            headlineSmall = tvStyle(28f, 36f, FontWeight.SemiBold),
            titleLarge = tvStyle(24f, 32f, FontWeight.SemiBold),
            titleMedium = tvStyle(22f, 30f, FontWeight.Medium),
            titleSmall = tvStyle(20f, 28f, FontWeight.Medium),
            bodyLarge = tvStyle(20f, 30f),
            bodyMedium = tvStyle(18f, 26f),
            bodySmall = tvStyle(16f, 24f),
            labelLarge = tvStyle(20f, 28f, FontWeight.Medium),
            labelMedium = tvStyle(16f, 24f, FontWeight.Medium),
            labelSmall = tvStyle(14f, 20f, FontWeight.Medium),
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(tvSize(4f)),
            small = RoundedCornerShape(tvSize(8f)),
            medium = RoundedCornerShape(tvSize(16f)),
            large = RoundedCornerShape(tvSize(24f)),
            extraLarge = RoundedCornerShape(tvSize(32f)),
        ),
        content = content,
    )
}

@Composable private fun tvStyle(size: Float, height: Float, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = FontFamily.SansSerif, fontSize = tvFont(size), lineHeight = tvFont(height), fontWeight = weight)
