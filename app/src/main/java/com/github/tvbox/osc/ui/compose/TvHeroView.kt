@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.github.tvbox.osc.ui.compose

import android.content.Context
import android.text.Html
import android.util.AttributeSet
import android.widget.ImageView
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.github.tvbox.osc.R
import com.github.tvbox.osc.modern.home.HomeUiState
import com.github.tvbox.osc.modern.home.HomeViewModel
import com.github.tvbox.osc.util.ImgUtil
import me.jessyan.autosize.utils.AutoSizeUtils

class TvHeroView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AbstractComposeView(context, attrs) {
    private var model by mutableStateOf<HomeViewModel?>(null)
    private var open: Runnable? = null
    init { setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed) }
    fun bind(value: HomeViewModel, action: Runnable) { model = value; open = action }

    @Composable override fun Content() {
        val viewModel = model ?: return
        val state by viewModel.state.collectAsStateWithLifecycle()
        TvTheme { Hero(state) { open?.run() } }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    @Composable private fun Hero(state: HomeUiState, onOpen: () -> Unit) {
        val configured = state.selection.selected != null
        val hero = state.hero
        val title = hero?.title ?: state.selection.selected?.name ?: stringResource(R.string.tv_empty_title)
        val metadata = hero?.let { listOfNotNull(it.year.takeIf { year -> year > 0 }?.toString(), it.type.takeIf(String::isNotEmpty)).joinToString(" · ") }.orEmpty()
        val description = if (hero == null) stringResource(if (configured) R.string.tv_recommendation_empty else R.string.tv_empty_description)
            else Html.fromHtml(hero.description, Html.FROM_HTML_MODE_LEGACY).toString().trim().ifEmpty { hero.note }
                .ifEmpty { stringResource(R.string.tv_recommendation_empty) }
        val label = stringResource(if (!configured) R.string.tv_add_config else if (hero == null) R.string.tv_search else R.string.tv_view_details)
        Row(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Button(onOpen, Modifier.width(tvSize(612f)).fillMaxHeight().testTag("com.coke.tv:id/tvHeroArtwork")
                .then(if (focused) Modifier.border(2.dp, colorResource(R.color.tv_outline), RoundedCornerShape(20.dp)) else Modifier),
                interactionSource = interaction, contentPadding = PaddingValues(0.dp),
                shape = ButtonDefaults.shape(RoundedCornerShape(20.dp)),
                border = ButtonDefaults.border(focusedBorder = Border.None),
                scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f)) {
                AndroidView(factory = { ImageView(it) }, modifier = Modifier.fillMaxSize(), update = { image ->
                    val key = hero?.poster.orEmpty() + "|" + title
                    if (image.tag != key) {
                        image.tag = key
                        val poster = hero?.poster.orEmpty()
                        when {
                            poster.isEmpty() -> { image.scaleType = ImageView.ScaleType.FIT_XY; image.setImageResource(R.drawable.tv_landscape_placeholder) }
                            ImgUtil.isBase64Image(poster) -> { image.scaleType = ImageView.ScaleType.CENTER_CROP; image.setImageBitmap(ImgUtil.decodeBase64ToBitmap(poster)) }
                            else -> { image.scaleType = ImageView.ScaleType.CENTER_CROP; ImgUtil.load(poster, image,
                                AutoSizeUtils.mm2px(context, 20f), AutoSizeUtils.mm2px(context, 612f), AutoSizeUtils.mm2px(context, 292f), title) }
                        }
                    }
                })
            }
            Spacer(Modifier.width(tvSize(36f)))
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                TvText(stringResource(if (configured) R.string.tv_source_recommendations else R.string.app_name), 16f, color = colorResource(R.color.tv_accent))
                Spacer(Modifier.height(tvSize(12f)))
                TvText(title, 44f, bold = true)
                Spacer(Modifier.height(tvSize(12f)))
                TvText(metadata, 17f, color = colorResource(R.color.tv_text_secondary))
                Spacer(Modifier.height(tvSize(15f)))
                TvText(description, 18f, color = colorResource(R.color.tv_text_secondary), maxLines = 2, lineSpacing = 5f)
                Spacer(Modifier.height(tvSize(22f)))
                TvPill(label, Modifier.width(tvSize(160f)).height(tvSize(44f)).testTag("com.coke.tv:id/tvHeroPlay"),
                    primary = true, onClick = onOpen)
            }
        }
    }
}
