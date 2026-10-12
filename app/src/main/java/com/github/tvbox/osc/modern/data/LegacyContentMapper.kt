package com.github.tvbox.osc.modern.data

import com.github.tvbox.osc.bean.Movie
import com.github.tvbox.osc.modern.model.*

internal fun Movie.Video.toContent(source: String) = ContentItem(
    ref = ContentRef(sourceKey.orEmpty().ifEmpty { source }, id.orEmpty()),
    title = name.orEmpty(), poster = pic.orEmpty(), year = year,
    type = type.orEmpty(), description = des.orEmpty(), note = note.orEmpty(),
    action = action.orEmpty(), tag = tag.orEmpty(),
)

internal fun Movie.Video.toDetails(source: String) = ContentDetails(
    item = toContent(source), actor = actor.orEmpty(), director = director.orEmpty(),
    lines = urlBean?.infoList.orEmpty().map { line ->
        val episodes = line.urls.orEmpty().split('#').filter { it.isNotEmpty() }.map { value ->
            val separator = value.indexOf('$')
            if (separator >= 0) Episode(value.substring(0, separator), value.substring(separator + 1))
            else Episode(value, value)
        }
        PlaybackLine(line.flag.orEmpty(), episodes)
    },
)
