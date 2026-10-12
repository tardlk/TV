package com.github.tvbox.osc.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.*
import com.github.tvbox.osc.R
import com.github.tvbox.osc.base.BaseActivity
import com.github.tvbox.osc.event.RefreshEvent
import com.github.tvbox.osc.modern.data.*
import com.github.tvbox.osc.modern.library.*
import com.github.tvbox.osc.util.HawkConfig
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class LibraryActivity : BaseActivity() {
    protected abstract val mode: LibraryMode
    private lateinit var model: LibraryViewModel
    override fun getLayoutResID() = R.layout.activity_modern_library
    override fun shouldRefreshAutoSize() = mode == LibraryMode.History
    override fun init() {
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == LibraryViewModel::class.java)
                return LibraryViewModel(mode, LegacyLibraryStore(), LegacySourceAccess(), LegacySettingsStore()) as T
            }
        })[LibraryViewModel::class.java]
        EventBus.getDefault().register(this)
        val content = LegacyLibraryView(this, mode, if (isBaseOnWidth) 5 else 6, model)
        findViewById<android.widget.FrameLayout>(R.id.libraryContent).addView(content)
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            model.state.collect(content::render)
        } }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) {
            model.navigation.collect { action ->
                when (action) {
                    LibraryNavigation.OperationFailed -> android.widget.Toast.makeText(this@LibraryActivity, "操作未完成，请重试", android.widget.Toast.LENGTH_SHORT).show()
                    is LibraryNavigation.Details -> jumpActivity(DetailActivity::class.java, Bundle().apply {
                        putString("id", action.item.ref.id); putString("sourceKey", action.item.ref.sourceKey)
                        putString("title", action.item.title); putString("picture", action.item.poster)
                        if (action.fromCollection) putBoolean("collect", true)
                    })
                    is LibraryNavigation.Search -> {
                        val target = if (action.fast) FastSearchActivity::class.java else SearchActivity::class.java
                        if (action.clearTop) startActivity(Intent(this@LibraryActivity, target).putExtra("title", action.title)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        else jumpActivity(target, Bundle().apply { putString("title", action.title) })
                    }
                }
            }
        } }
    }
    override fun onResume() { super.onResume(); if (::model.isInitialized) model.refresh() }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refresh(event: RefreshEvent) { if (event.type == RefreshEvent.TYPE_HISTORY_REFRESH) model.refresh() }
    override fun onBackPressed() {
        if (model.state.value.deleteMode) model.toggleDelete() else super.onBackPressed()
    }
    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        HawkConfig.hotVodDelete = false
        super.onDestroy()
    }
}
