package com.github.tvbox.osc.modern.data

import com.github.tvbox.osc.modern.model.*

interface SourceAccess {
    fun selection(): SourceSelection
    suspend fun catalog(sourceKey: String): SourceCatalog
    suspend fun search(sourceKey: String, query: String): List<ContentItem>
    suspend fun details(ref: ContentRef): ContentDetails
    suspend fun resolve(ref: ContentRef, flag: String, url: String, progressKey: String, subtitleKey: String): ResolvedPlayback
}

class SourceUnavailable(cause: Throwable? = null) : RuntimeException("Source returned no usable response", cause)
