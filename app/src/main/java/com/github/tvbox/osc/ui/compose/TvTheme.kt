package com.github.tvbox.osc.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.github.tvbox.osc.R

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = colorResource(R.color.tv_accent),
            onPrimary = colorResource(R.color.tv_on_accent),
            background = colorResource(R.color.tv_background),
            onBackground = colorResource(R.color.tv_text_primary),
            surface = colorResource(R.color.tv_surface),
            onSurface = colorResource(R.color.tv_text_primary),
        ),
        content = content,
    )
}
