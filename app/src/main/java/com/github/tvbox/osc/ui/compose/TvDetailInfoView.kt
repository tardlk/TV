package com.github.tvbox.osc.ui.compose

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.tvbox.osc.R
import com.github.tvbox.osc.modern.detail.DetailViewModel

abstract class DetailTextView(context: Context, attrs: AttributeSet?, private val descriptionOnly: Boolean) : AbstractComposeView(context, attrs) {
    private var model by mutableStateOf<DetailViewModel?>(null)
    init {
        isFocusable = false
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }
    fun bind(value: DetailViewModel) { model = value }
    @Composable override fun Content() {
        val state by (model ?: return).state.collectAsStateWithLifecycle()
        val info = state.info
        TvTheme {
            if (descriptionOnly) Field("内容简介：", info.description, 17f, 3)
            else Column(Modifier.fillMaxWidth()) {
                TvText(info.title, 38f, Modifier.fillMaxWidth())
                Spacer(Modifier.height(tvSize(5f)))
                Row(Modifier.fillMaxWidth()) {
                    listOf("来源：" to info.source, "年份：" to info.year, "地区：" to info.area,
                        "语言：" to info.language, "类型：" to info.type).forEach { (label, value) ->
                        if (value.isNotEmpty()) Box(Modifier.padding(end = tvSize(10f))) { Field(label, value) }
                    }
                }
                Spacer(Modifier.height(tvSize(5f)))
                if (info.actor.isNotEmpty()) { Field("演员：", info.actor); Spacer(Modifier.height(tvSize(5f))) }
                if (info.director.isNotEmpty()) { Field("导演：", info.director); Spacer(Modifier.height(tvSize(5f))) }
            }
        }
    }
    @Composable private fun Field(label: String, value: String, size: Float = 20f, lines: Int = 1) {
        if (value.isEmpty()) return
        val primary = colorResource(R.color.tv_text_primary)
        val secondary = colorResource(R.color.tv_text_secondary)
        val text = buildAnnotatedString { append(label); withStyle(SpanStyle(color = primary)) { append(value) } }
        TvText("", size, color = secondary, maxLines = lines, lineSpacing = if (lines > 1) 3f else 0f,
            lineMultiplier = if (lines > 1) 1.2f else 1f, styled = text)
    }
}
class TvDetailInfoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : DetailTextView(context, attrs, false)
class TvDetailDescriptionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : DetailTextView(context, attrs, true)
