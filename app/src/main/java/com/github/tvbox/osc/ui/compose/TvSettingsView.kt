@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.github.tvbox.osc.ui.compose

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.*
import com.github.tvbox.osc.R

/** Values supplied by the existing settings coordinator; no second storage or action owner. */
@Stable
class TvSettingValue {
    var text by mutableStateOf("")
        private set
    fun setText(value: CharSequence?) { text = value?.toString().orEmpty() }
}

class TvSettingsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AbstractComposeView(context, attrs) {
    private val values = TvSettingsCatalog.rows.filter { it.valueId != 0 }.associate { it.valueId to TvSettingValue() }
    private val visible = mutableStateMapOf<Int, Boolean>().apply { TvSettingsCatalog.rows.forEach { put(it.actionId, it.visible) } }
    private val actions = mutableMapOf<Int, Runnable>()
    private val lastConfirm = mutableMapOf<Int, Long>()
    init { setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed) }
    fun value(id: Int): TvSettingValue = requireNotNull(values[id]) { "Unknown settings value: $id" }
    fun bindAction(id: Int, action: Runnable) {
        require(TvSettingsCatalog.rows.any { it.actionId == id }) { "Unknown settings action: $id" }
        actions[id] = action
    }
    fun showAction(id: Int, show: Boolean) { visible[id] = show }
    fun releaseActions() { actions.clear(); lastConfirm.clear() }

    private fun confirm(id: Int) {
        val action = actions[id] ?: return
        val now = SystemClock.uptimeMillis()
        if (now - (lastConfirm[id] ?: -500L) < 500) return
        lastConfirm[id] = now
        action.run()
    }

    @Composable override fun Content() {
        TvTheme {
            var group by rememberSaveable { mutableIntStateOf(0) }
            val groupFocus = remember { List(TvSettingsCatalog.groups.size) { FocusRequester() } }
            val rowFocus = remember { TvSettingsCatalog.rows.associate { it.actionId to FocusRequester() } }
            val colors = MaterialTheme.colorScheme
            Column(Modifier.fillMaxSize().background(colors.background)
                .padding(horizontal = tvSize(48f), vertical = tvSize(24f))
                .semantics { testTagsAsResourceId = true }) {
                Text(stringResource(R.string.tv_settings_title), style = MaterialTheme.typography.headlineLarge,
                    color = colors.onBackground, modifier = Modifier.testTag("com.coke.tv:id/tvSettingsHeading"))
                Spacer(Modifier.height(tvSize(4f)))
                Text(stringResource(R.string.tv_settings_description), style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant)
                Spacer(Modifier.height(tvSize(24f)))
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(tvSize(28f))) {
                    Column(Modifier.width(tvSize(230f)), verticalArrangement = Arrangement.spacedBy(tvSize(12f))) {
                        TvSettingsCatalog.groups.forEachIndexed { index, section ->
                            val first = section.rows.first { visible[it.actionId] == true }
                            TvPill(stringResource(section.title), Modifier.fillMaxWidth().height(tvSize(56f))
                                .testTag("com.coke.tv:id/tvSettingsGroup$index")
                                .focusRequester(groupFocus[index])
                                .focusProperties { right = rowFocus.getValue(first.actionId) }
                                .onFocusChanged { if (it.isFocused) group = index },
                                selected = group == index, textPrimary = true,
                                onClick = { group = index; rowFocus.getValue(first.actionId).requestFocus() })
                        }
                    }
                    key(group) {
                        val rows = TvSettingsCatalog.groups[group].rows.filter { visible[it.actionId] == true }
                        LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(tvSize(6f)),
                            verticalArrangement = Arrangement.spacedBy(tvSize(4f))) {
                            itemsIndexed(rows, key = { _, row -> row.actionId }) { index, row ->
                                SettingsRow(row, index, rows.size, Modifier
                                    .focusRequester(rowFocus.getValue(row.actionId))
                                    .focusProperties { left = groupFocus[group] })
                            }
                        }
                    }
                }
            }
            LaunchedEffect(Unit) { groupFocus[group].requestFocus() }
        }
    }

    @Composable private fun SettingsRow(row: TvSettingSpec, index: Int, count: Int, modifier: Modifier) {
        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        val colors = MaterialTheme.colorScheme
        val radius = tvSize(24f)
        val seam = tvSize(4f)
        val shape = RoundedCornerShape(
            topStart = if (index == 0) radius else seam, topEnd = if (index == 0) radius else seam,
            bottomStart = if (index == count - 1) radius else seam, bottomEnd = if (index == count - 1) radius else seam)
        Button(onClick = { confirm(row.actionId) },
            modifier = modifier.fillMaxWidth().height(tvSize(82f))
                .testTag("com.coke.tv:id/${resources.getResourceEntryName(row.actionId)}")
                .then(if (focused) Modifier.border(tvSize(3f), colors.border, shape) else Modifier),
            interactionSource = interaction, shape = ButtonDefaults.shape(shape),
            border = ButtonDefaults.border(focusedBorder = Border.None),
            scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = .99f),
            colors = ButtonDefaults.colors(containerColor = colors.surface, contentColor = colors.onSurface,
                focusedContainerColor = colors.primaryContainer, focusedContentColor = colors.onPrimaryContainer),
            contentPadding = PaddingValues(horizontal = tvSize(24f), vertical = tvSize(10f))) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tvSize(20f))) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(row.title), style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(tvSize(3f)))
                    Text(stringResource(row.description), style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (row.valueId != 0) {
                    val text = value(row.valueId).text
                    Text(text.ifEmpty { if (row.actionId == R.id.llApi) stringResource(R.string.tv_settings_empty_value) else "" },
                        modifier = Modifier.widthIn(max = tvSize(350f))
                            .testTag("com.coke.tv:id/${resources.getResourceEntryName(row.valueId)}"),
                        style = MaterialTheme.typography.bodyMedium, color = colors.primary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("›", style = MaterialTheme.typography.headlineSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
