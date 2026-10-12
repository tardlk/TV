package com.github.tvbox.osc.ui.compose

import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import com.github.tvbox.osc.R
import me.jessyan.autosize.utils.AutoSizeUtils

@Composable fun tvSize(mm: Float): Dp = with(LocalDensity.current) { AutoSizeUtils.mm2px(LocalContext.current, mm).toDp() }
@Composable fun tvFont(mm: Float): TextUnit = with(LocalDensity.current) {
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, mm, LocalContext.current.resources.displayMetrics).toSp()
}

@Composable
fun TvText(value: String, size: Float, modifier: Modifier = Modifier, bold: Boolean = false,
           color: androidx.compose.ui.graphics.Color = colorResource(R.color.tv_text_primary),
           maxLines: Int = 1, align: TextAlign = TextAlign.Start, lineSpacing: Float = 0f) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, size, context.resources.displayMetrics)
    val extra = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, lineSpacing, context.resources.displayMetrics)
    val metrics = remember(pixels, bold) {
        Paint().apply { textSize = pixels; typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL) }.fontMetricsInt
    }
    val style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = with(density) { pixels.toSp() },
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color,
        textAlign = align, platformStyle = PlatformTextStyle(includeFontPadding = true),
        lineHeight = if (maxLines > 1) with(density) { (metrics.descent - metrics.ascent + extra).toSp() } else TextUnit.Unspecified)
    Layout(modifier = modifier, content = {
        Text(value, maxLines = maxLines, overflow = TextOverflow.Ellipsis, style = style)
    }) { children, constraints ->
        val text = children.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val first = text[FirstBaseline]
        val last = text[LastBaseline]
        val height = constraints.constrainHeight(metrics.bottom - metrics.top + (last - first).coerceAtLeast(0))
        layout(text.width, height) { text.place(0, -metrics.top - first) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvPill(text: String, modifier: Modifier = Modifier, selected: Boolean = false,
           primary: Boolean = false, textPrimary: Boolean = false, textSize: Float = 20f, horizontalPadding: Float = 0f,
           onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val accent = colorResource(R.color.tv_accent)
    val selectedColor = colorResource(R.color.tv_selected)
    val foreground = colorResource(if (primary) R.color.tv_on_accent else if (textPrimary) R.color.tv_text_primary else R.color.tv_text_secondary)
    val shape = RoundedCornerShape(100.dp)
    Button(onClick, modifier = modifier.then(if (focused) Modifier.border(if (primary) 3.dp else 2.dp,
        colorResource(R.color.tv_outline), shape) else Modifier), onLongClick = onLongClick,
        interactionSource = interaction, contentPadding = PaddingValues(horizontal = tvSize(horizontalPadding)),
        shape = ButtonDefaults.shape(shape),
        border = ButtonDefaults.border(focusedBorder = Border.None),
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        colors = ButtonDefaults.colors(
            containerColor = if (primary) accent else if (selected) selectedColor else colorResource(R.color.tv_surface),
            contentColor = foreground, focusedContainerColor = if (primary) accent else selectedColor,
            focusedContentColor = foreground, pressedContainerColor = if (primary) accent else selectedColor,
            pressedContentColor = foreground,
        )) {
        TvText(text, textSize, modifier = Modifier.fillMaxWidth(), color = foreground, bold = primary, align = TextAlign.Center)
    }
}
