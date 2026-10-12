package com.github.tvbox.osc.modern.data

import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.modern.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LibraryStore {
    suspend fun recent(): List<ContentItem>
    suspend fun collections(): List<CollectionEntry>
    suspend fun removeRecent(ref: ContentRef)
    suspend fun clearRecent()
    suspend fun setCollected(item: ContentItem, collected: Boolean)
    suspend fun clearCollections()
}

class LegacyLibraryStore : LibraryStore {
    override suspend fun recent(): List<ContentItem> = withContext(Dispatchers.IO) {
        RoomDataManger.getAllVodRecord(20).map { record ->
            ContentItem(ContentRef(record.sourceKey.orEmpty(), record.id.orEmpty()), record.name.orEmpty(),
                poster = record.pic.orEmpty(), note = record.playNote.orEmpty().let { if (it.isEmpty()) "" else "上次看到 $it" })
        }
    }
    override suspend fun collections(): List<CollectionEntry> = withContext(Dispatchers.IO) {
        RoomDataManger.getAllVodCollect().map { record ->
            CollectionEntry(record.id, ContentItem(ContentRef(record.sourceKey.orEmpty(), record.vodId.orEmpty()),
                record.name.orEmpty(), poster = record.pic.orEmpty()))
        }
    }
    override suspend fun removeRecent(ref: ContentRef): Unit = withContext(Dispatchers.IO) {
        RoomDataManger.getVodInfo(ref.sourceKey, ref.id)?.let { RoomDataManger.deleteVodRecord(ref.sourceKey, it) }
    }
    override suspend fun clearRecent(): Unit = withContext(Dispatchers.IO) { RoomDataManger.deleteVodRecordAll() }
    override suspend fun setCollected(item: ContentItem, collected: Boolean): Unit = withContext(Dispatchers.IO) {
        val legacy = VodInfo().apply { id = item.ref.id; name = item.title; pic = item.poster }
        if (collected) RoomDataManger.insertVodCollect(item.ref.sourceKey, legacy)
        else RoomDataManger.deleteVodCollect(item.ref.sourceKey, legacy)
    }
    override suspend fun clearCollections(): Unit = withContext(Dispatchers.IO) { RoomDataManger.deleteVodCollectAll() }
}
