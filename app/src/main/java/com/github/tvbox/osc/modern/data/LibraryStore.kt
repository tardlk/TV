package com.github.tvbox.osc.modern.data

import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.modern.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LibraryStore {
    suspend fun recent(limit: Int = 20): List<HistoryEntry>
    suspend fun isCollected(ref: ContentRef): Boolean = collections().any { it.item.ref == ref }
    suspend fun collections(): List<CollectionEntry>
    suspend fun removeRecent(ref: ContentRef)
    suspend fun clearRecent()
    suspend fun setCollected(item: ContentItem, collected: Boolean)
    suspend fun clearCollections()
}

class LegacyLibraryStore : LibraryStore {
    override suspend fun recent(limit: Int): List<HistoryEntry> = withContext(Dispatchers.IO) {
        RoomDataManger.getAllVodRecord(limit).map { record ->
            HistoryEntry(ContentItem(ContentRef(record.sourceKey.orEmpty(), record.id.orEmpty()), record.name.orEmpty(),
                poster = record.pic.orEmpty(), note = record.note.orEmpty()), record.playNote.orEmpty())
        }
    }
    override suspend fun isCollected(ref: ContentRef): Boolean = withContext(Dispatchers.IO) { RoomDataManger.isVodCollect(ref.sourceKey, ref.id) }
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
