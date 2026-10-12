package com.github.tvbox.osc.modern.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.github.tvbox.osc.R
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.VodCollect
import com.github.tvbox.osc.ui.adapter.CollectAdapter
import com.github.tvbox.osc.ui.adapter.HistoryAdapter
import com.github.tvbox.osc.ui.dialog.ConfirmClearDialog
import com.github.tvbox.osc.util.FastClickCheckUtil
import com.github.tvbox.osc.util.HawkConfig
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.owen.tvrecyclerview.widget.V7GridLayoutManager

class LegacyLibraryView(context: Context, private val mode: LibraryMode, spanCount: Int,
                        private val model: LibraryViewModel) : FrameLayout(context) {
    private val content = LayoutInflater.from(context).inflate(if (mode == LibraryMode.History) R.layout.activity_history else R.layout.activity_collect, this, true)
    private val grid = content.findViewById<TvRecyclerView>(R.id.mGridView)
    private val remove = content.findViewById<ImageView>(R.id.tvDelete)
    private val clear = content.findViewById<ImageView>(R.id.tvClear)
    private val tip = content.findViewById<TextView>(R.id.tvDelTip)
    private val history = if (mode == LibraryMode.History) HistoryAdapter() else null
    private val collection = if (mode == LibraryMode.Collection) CollectAdapter() else null
    private var rows = emptyList<LibraryRow>()
    private var deleteMode = false

    init {
        grid.setHasFixedSize(true)
        grid.layoutManager = V7GridLayoutManager(context, spanCount)
        grid.adapter = history ?: collection
        remove.setOnClickListener { model.toggleDelete() }
        clear.setOnClickListener { ConfirmClearDialog(context, Runnable { model.clear() }).show() }
        grid.setOnInBorderKeyEventListener { direction, _ ->
            if (direction == View.FOCUS_UP) { remove.isFocusable = true; clear.isFocusable = true; remove.requestFocus() }
            false
        }
        grid.setOnItemListener(object : TvRecyclerView.OnItemListener {
            override fun onItemPreSelected(parent: TvRecyclerView?, item: View?, position: Int) {
                item?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(300)?.setInterpolator(BounceInterpolator())?.start()
            }
            override fun onItemSelected(parent: TvRecyclerView?, item: View?, position: Int) {
                item?.animate()?.scaleX(1.05f)?.scaleY(1.05f)?.setDuration(300)?.setInterpolator(BounceInterpolator())?.start()
            }
            override fun onItemClick(parent: TvRecyclerView?, item: View?, position: Int) = Unit
        })
        val adapter = history ?: collection!!
        adapter.setOnItemClickListener { _, view, position ->
            FastClickCheckUtil.check(view)
            rows.getOrNull(position)?.let(model::open)
        }
        adapter.setOnItemLongClickListener { _, _, position -> rows.getOrNull(position)?.let(model::search); true }
    }

    fun render(state: LibraryUiState) {
        HawkConfig.hotVodDelete = state.deleteMode
        if (state.deleteMode != deleteMode) {
            deleteMode = state.deleteMode
            HawkConfig.hotVodDelete = deleteMode
            tip.visibility = if (deleteMode) View.VISIBLE else View.GONE
            (history ?: collection)?.notifyDataSetChanged()
        }
        if (rows != state.rows) {
            rows = state.rows
            history?.setNewData(rows.map { row -> VodInfo().apply {
                id = row.item.ref.id; sourceKey = row.item.ref.sourceKey; name = row.item.title; pic = row.item.poster; note = row.item.note
            } })
            collection?.setNewData(rows.map { row -> VodCollect().apply {
                id = row.recordId; vodId = row.item.ref.id; sourceKey = row.item.ref.sourceKey; name = row.item.title; pic = row.item.poster
            } })
        }
    }
}
