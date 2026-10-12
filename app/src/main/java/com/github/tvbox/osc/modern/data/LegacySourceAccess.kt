package com.github.tvbox.osc.modern.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.github.tvbox.osc.bean.AbsXml
import com.github.tvbox.osc.event.RefreshEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.modern.model.*
import com.github.tvbox.osc.viewmodel.SourceViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID

class LegacySourceAccess : SourceAccess {
    override fun selection(): SourceSelection {
        val config = ApiConfig.get()
        val options = config.sourceBeanList.map { SourceOption(it.key.orEmpty(), it.name.orEmpty()) }
        return SourceSelection(options, options.firstOrNull { it.key == config.homeSourceBean.key })
    }

    override suspend fun catalog(sourceKey: String): SourceCatalog {
        val source = SourceViewModel()
        val result = awaitResult(source.sortResult) { source.getSort(sourceKey) }
        val videos = result.videoList ?: result.list?.videoList.orEmpty()
        return SourceCatalog(sourceKey,
            result.classes?.sortList.orEmpty().map { Category(it.id.orEmpty(), it.name.orEmpty()) },
            videos.map { it.toContent(sourceKey) })
    }

    override suspend fun search(sourceKey: String, query: String): List<ContentItem> {
        val source = SourceViewModel()
        val token = UUID.randomUUID().toString()
        val result = awaitSignal<AbsXml>({ completion ->
            val observer = LegacySearchObserver(sourceKey, token) { completion.complete(it) }
            EventBus.getDefault().register(observer)
            val unsubscribe = { EventBus.getDefault().unregister(observer) }
            unsubscribe
        }) { source.getSearch(sourceKey, query, token) }
        return result.movie?.videoList.orEmpty().map { it.toContent(sourceKey) }
    }

    override suspend fun details(ref: ContentRef): ContentDetails {
        val source = SourceViewModel()
        val result = awaitResult(source.detailResult) { source.getDetail(ref.sourceKey, ref.id) }
        return result.movie?.videoList?.firstOrNull()?.toDetails(ref.sourceKey) ?: throw SourceUnavailable()
    }

    override suspend fun resolve(ref: ContentRef, flag: String, url: String, progressKey: String, subtitleKey: String): ResolvedPlayback {
        val source = SourceViewModel()
        return try {
            val result = awaitResult(source.playResult) { source.getPlay(ref.sourceKey, flag, progressKey, url, subtitleKey) }
            ResolvedPlayback(result.toString())
        } finally {
            source.cancelPlayRequest()
        }
    }

    private suspend fun <T : Any> awaitResult(result: LiveData<T>, request: () -> Unit): T = awaitSignal({ completion ->
        val observer = Observer<T> { value ->
            if (value == null) completion.completeExceptionally(SourceUnavailable()) else completion.complete(value)
        }
        result.observeForever(observer)
        val unsubscribe = { result.removeObserver(observer) }
        unsubscribe
    }, request)

    private suspend fun <T : Any> awaitSignal(subscribe: (CompletableDeferred<T>) -> (() -> Unit), request: () -> Unit): T = withContext(Dispatchers.Main.immediate) {
        val completion = CompletableDeferred<T>()
        val unsubscribe = subscribe(completion)
        val worker = SourceViewModel.spThreadPool.submit {
            try {
                request()
            } catch (failure: Throwable) {
                completion.completeExceptionally(SourceUnavailable(failure))
            }
        }
        try {
            withTimeout(35_000) { completion.await() }
        } finally {
            unsubscribe()
            worker.cancel(true)
            completion.cancel()
        }
    }
}

class LegacySearchObserver(
    private val sourceKey: String,
    private val token: String,
    private val receive: (AbsXml) -> Unit,
) {
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefresh(event: RefreshEvent) {
        if (event.type != RefreshEvent.TYPE_SEARCH_RESULT) return
        val response = event.obj as? AbsXml ?: return
        if (response.sourceKey == sourceKey && response.searchToken == token) receive(response)
    }
}
