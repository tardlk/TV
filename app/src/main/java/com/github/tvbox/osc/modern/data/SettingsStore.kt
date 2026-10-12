package com.github.tvbox.osc.modern.data

import com.github.tvbox.osc.util.HawkConfig
import com.orhanobut.hawk.Hawk

data class HomePreferences(val recommendationMode: Int = 1, val historyGrid: Boolean = false, val fastSearch: Boolean = true)

interface SettingsStore {
    fun homePreferences(): HomePreferences
    fun saveHomePreferences(value: HomePreferences)
}

class LegacySettingsStore : SettingsStore {
    override fun homePreferences() = HomePreferences(
        Hawk.get(HawkConfig.HOME_REC, HawkConfig.DEFAULT_HOME_REC),
        Hawk.get(HawkConfig.HOME_REC_STYLE, false),
        Hawk.get(HawkConfig.FAST_SEARCH_MODE, true),
    )
    override fun saveHomePreferences(value: HomePreferences) {
        Hawk.put(HawkConfig.HOME_REC, value.recommendationMode)
        Hawk.put(HawkConfig.HOME_REC_STYLE, value.historyGrid)
        Hawk.put(HawkConfig.FAST_SEARCH_MODE, value.fastSearch)
    }
}
