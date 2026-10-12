package com.github.tvbox.osc.modern.detail

import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

object DetailUiBindings {
    @JvmStatic fun bind(owner: LifecycleOwner, model: DetailViewModel, collect: TextView) {
        collect.setOnClickListener { model.toggleCollected() }
        owner.lifecycleScope.launch { owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { model.state.collect { state ->
                collect.text = if (state.collected) "取消收藏" else "加入收藏"
                collect.isEnabled = state.collectionKnown && state.item != null
            } }
            launch { model.notices.collect { notice ->
                Toast.makeText(collect.context, when (notice) {
                    DetailNotice.Added -> "已加入收藏夹"
                    DetailNotice.Removed -> "已移除收藏夹"
                    DetailNotice.Failed -> "收藏操作未完成，请重试"
                }, Toast.LENGTH_SHORT).show()
            } }
        } }
    }
}
