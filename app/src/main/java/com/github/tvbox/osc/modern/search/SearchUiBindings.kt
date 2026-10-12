package com.github.tvbox.osc.modern.search

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

object SearchUiBindings {
    fun interface Renderer { fun render(state: SearchUiState) }
    fun interface Notices { fun show(notice: SearchNotice) }
    @JvmStatic fun bind(owner: LifecycleOwner, model: SearchViewModel, renderer: Renderer, notices: Notices) {
        owner.lifecycleScope.launch { owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { model.state.collect { renderer.render(it) } }
            launch { model.notices.collect { notices.show(it) } }
        } }
    }
}
