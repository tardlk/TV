package com.github.tvbox.osc.modern.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.modern.data.*
import com.github.tvbox.osc.modern.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HomeLoad { Empty, Loading, Ready, Failed }

data class HomeUiState(
    val selection: SourceSelection = SourceSelection(),
    val load: HomeLoad = HomeLoad.Empty,
    val recommendations: List<ContentItem> = emptyList(),
    val recent: List<ContentItem> = emptyList(),
    val preferences: HomePreferences = HomePreferences(),
) {
    val hero: ContentItem? get() = if (selection.selected == null) null else recommendations.firstOrNull { it.title.isNotEmpty() } ?: recent.firstOrNull()
}

class HomeViewModel(
    private val sources: SourceAccess,
    private val library: LibraryStore,
    private val settings: SettingsStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val state = mutableState.asStateFlow()
    private var generation = 0L
    private var refreshJob: Job? = null

    fun refresh(seed: SourceCatalog? = null) {
        val request = ++generation
        refreshJob?.cancel()
        val selection = sources.selection()
        mutableState.value = HomeUiState(selection = selection,
            load = if (selection.selected == null) HomeLoad.Empty else HomeLoad.Loading,
            preferences = settings.homePreferences())
        refreshJob = viewModelScope.launch {
            try {
                val recent = library.recent().map { it.item.copy(note = if (it.playedNote.isEmpty()) "" else "上次看到 ${it.playedNote}") }
                if (request != generation) return@launch
                mutableState.value = mutableState.value.copy(recent = recent)
                val selected = selection.selected ?: return@launch
                val catalog = seed?.takeIf { it.sourceKey == selected.key } ?: sources.catalog(selected.key)
                if (request != generation) return@launch
                require(catalog.sourceKey == selected.key) { "Catalog belongs to another source" }
                mutableState.value = mutableState.value.copy(
                    load = if (catalog.recommendations.isEmpty()) HomeLoad.Empty else HomeLoad.Ready,
                    recommendations = catalog.recommendations.toList(),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (request == generation) mutableState.value = mutableState.value.copy(load = HomeLoad.Failed)
            }
        }
    }

    fun cancelRefresh() {
        generation++
        refreshJob?.cancel()
        if (mutableState.value.load == HomeLoad.Loading) mutableState.value = mutableState.value.copy(load = HomeLoad.Empty)
    }
}
