@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.github.tvbox.osc.ui.compose

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.Alignment
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.tvbox.osc.R

class TvHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AbstractComposeView(context, attrs) {
    fun interface NavigationListener { fun onNavigate(id: Int) }
    var sourceName by mutableStateOf(context.getString(R.string.tv_add_config))
    var clockText by mutableStateOf("")
    var sourceLoading by mutableStateOf(false)
    private var revision by mutableIntStateOf(0)
    private var navigation: NavigationListener? = null
    private var homeLong: View.OnLongClickListener? = null
    private var sourceLong: View.OnLongClickListener? = null
    private val homeFocus = FocusRequester()

    init { setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed) }
    fun setNavigationListener(value: NavigationListener) { navigation = value }
    fun setHomeLongClickListener(value: View.OnLongClickListener) { homeLong = value }
    fun setSourceLongClickListener(value: View.OnLongClickListener) { sourceLong = value }
    fun refreshSizes() { revision++ }
    fun requestHomeFocus() { post { if (isAttachedToWindow) homeFocus.requestFocus() } }

    @Composable override fun Content() {
        revision
        TvTheme {
            Row(Modifier.fillMaxSize().padding(horizontal = tvSize(48f)).semantics { testTagsAsResourceId = true },
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.app_name), Modifier.width(tvSize(82f)),
                    style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                val entries = listOf(R.id.tvNavHome to R.string.tv_home, R.id.tvNavLive to R.string.tv_live,
                    R.id.tvNavHistory to R.string.tv_history, R.id.tvNavCollection to R.string.tv_collection,
                    R.id.tvNavSettings to R.string.tv_settings)
                entries.forEach { (id, label) ->
                    val tag = resources.getResourceEntryName(id)
                    TvPill(stringResource(label), Modifier.width(tvSize(82f)).height(tvSize(44f))
                        .testTag("com.coke.tv:id/$tag").then(if (id == R.id.tvNavHome) Modifier.focusRequester(homeFocus) else Modifier),
                        selected = id == R.id.tvNavHome,
                        onLongClick = if (id == R.id.tvNavHome) ({ homeLong?.onLongClick(this@TvHeaderView); Unit }) else null,
                        onClick = { navigation?.onNavigate(id) })
                    Spacer(Modifier.width(tvSize(8f)))
                }
                Spacer(Modifier.weight(1f))
                val opacity = if (sourceLoading) {
                    val transition = rememberInfiniteTransition(label = "source-loading")
                    val alpha by transition.animateFloat(0f, 1f,
                        infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "source-opacity")
                    alpha
                } else 1f
                TvPill(sourceName, Modifier.width(tvSize(200f)).height(tvSize(44f)).alpha(opacity)
                    .testTag("com.coke.tv:id/tvNavSource"), textPrimary = true, horizontalPadding = 12f,
                    onLongClick = { sourceLong?.onLongClick(this@TvHeaderView) },
                    onClick = { navigation?.onNavigate(R.id.tvNavSource) })
                Spacer(Modifier.width(tvSize(12f)))
                TvPill(stringResource(R.string.tv_search), Modifier.width(tvSize(68f)).height(tvSize(44f))
                    .testTag("com.coke.tv:id/tvNavSearch"), onClick = { navigation?.onNavigate(R.id.tvNavSearch) })
                Spacer(Modifier.width(tvSize(8f)))
                Box(Modifier.width(tvSize(65f)).height(tvSize(44f)), contentAlignment = Alignment.Center) {
                    Text(clockText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
