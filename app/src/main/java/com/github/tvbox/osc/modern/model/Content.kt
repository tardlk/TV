package com.github.tvbox.osc.modern.model

data class ContentRef(val sourceKey: String, val id: String)

data class ContentItem(
    val ref: ContentRef,
    val title: String,
    val poster: String = "",
    val year: Int = 0,
    val type: String = "",
    val description: String = "",
    val note: String = "",
    val action: String = "",
    val tag: String = "",
)

data class SourceOption(val key: String, val name: String)
data class SourceSelection(val sources: List<SourceOption> = emptyList(), val selected: SourceOption? = null)
data class Category(val id: String, val name: String)
data class SourceCatalog(val sourceKey: String, val categories: List<Category>, val recommendations: List<ContentItem>)
data class Episode(val name: String, val url: String)
data class PlaybackLine(val flag: String, val episodes: List<Episode>)
data class ContentDetails(val item: ContentItem, val actor: String, val director: String, val lines: List<PlaybackLine>)
data class ResolvedPlayback(val responseJson: String)
data class CollectionEntry(val recordId: Int, val item: ContentItem)
