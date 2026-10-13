package com.github.tvbox.osc.ui.compose

import com.github.tvbox.osc.R

data class TvSettingSpec(val actionId: Int, val title: Int, val description: Int, val valueId: Int = 0, val visible: Boolean = true)
data class TvSettingsGroup(val title: Int, val rows: List<TvSettingSpec>)

object TvSettingsCatalog {
    val groups = listOf(
        TvSettingsGroup(R.string.tv_settings_group_0, listOf(
            TvSettingSpec(R.id.llApi, R.string.tv_setting_llapi, R.string.tv_setting_llapi_description, R.id.tvApi),
            TvSettingSpec(R.id.llApiHistory, R.string.tv_setting_llapihistory, R.string.tv_setting_llapihistory_description, R.id.tvMediaCodec1),
            TvSettingSpec(R.id.llApiLine, R.string.tv_setting_llapiline, R.string.tv_setting_llapiline_description, R.id.tvApiLine, visible = false),
            TvSettingSpec(R.id.llHomeApi, R.string.tv_setting_llhomeapi, R.string.tv_setting_llhomeapi_description, R.id.tvHomeApi),
            TvSettingSpec(R.id.llHomeRec, R.string.tv_setting_llhomerec, R.string.tv_setting_llhomerec_description, R.id.tvHomeRec),
            TvSettingSpec(R.id.llHomeRecStyle, R.string.tv_setting_llhomerecstyle, R.string.tv_setting_llhomerecstyle_description, R.id.showRecStyleText),
            TvSettingSpec(R.id.tvHomeLive, R.string.tv_setting_tvhomelive, R.string.tv_setting_tvhomelive_description, R.id.tvHomeText),
        )),
        TvSettingsGroup(R.string.tv_settings_group_1, listOf(
            TvSettingSpec(R.id.llPlay, R.string.tv_setting_llplay, R.string.tv_setting_llplay_description, R.id.tvPlay),
            TvSettingSpec(R.id.llMediaCodec, R.string.tv_setting_llmediacodec, R.string.tv_setting_llmediacodec_description, R.id.tvMediaCodec),
            TvSettingSpec(R.id.llRender, R.string.tv_setting_llrender, R.string.tv_setting_llrender_description, R.id.tvRenderType),
            TvSettingSpec(R.id.llScale, R.string.tv_setting_llscale, R.string.tv_setting_llscale_description, R.id.tvScaleType),
            TvSettingSpec(R.id.showPreview, R.string.tv_setting_showpreview, R.string.tv_setting_showpreview_description, R.id.showPreviewText),
            TvSettingSpec(R.id.autoSwitchLine, R.string.tv_setting_autoswitchline, R.string.tv_setting_autoswitchline_description, R.id.autoSwitchLineText),
            TvSettingSpec(R.id.llIjkCachePlay, R.string.tv_setting_llijkcacheplay, R.string.tv_setting_llijkcacheplay_description, R.id.tvIjkCachePlay),
            TvSettingSpec(R.id.m3u8Ad, R.string.tv_setting_m3u8ad, R.string.tv_setting_m3u8ad_description, R.id.m3u8AdText),
            TvSettingSpec(R.id.danmuOpen, R.string.tv_setting_danmuopen, R.string.tv_setting_danmuopen_description, R.id.danmuOpenText),
            TvSettingSpec(R.id.danmuApi, R.string.tv_setting_danmuapi, R.string.tv_setting_danmuapi_description, R.id.danmuApiText),
            TvSettingSpec(R.id.llParseWebVew, R.string.tv_setting_llparsewebvew, R.string.tv_setting_llparsewebvew_description, R.id.tvParseWebView),
        )),
        TvSettingsGroup(R.string.tv_settings_group_2, listOf(
            TvSettingSpec(R.id.llSearchView, R.string.tv_setting_llsearchview, R.string.tv_setting_llsearchview_description, R.id.tvSearchView),
            TvSettingSpec(R.id.showFastSearch, R.string.tv_setting_showfastsearch, R.string.tv_setting_showfastsearch_description, R.id.showFastSearchText),
            TvSettingSpec(R.id.llHistoryNum, R.string.tv_setting_llhistorynum, R.string.tv_setting_llhistorynum_description, R.id.tvHistoryNum),
            TvSettingSpec(R.id.llHistoryMerge, R.string.tv_setting_llhistorymerge, R.string.tv_setting_llhistorymerge_description, R.id.tvHistoryMerge),
            TvSettingSpec(R.id.llDns, R.string.tv_setting_lldns, R.string.tv_setting_lldns_description, R.id.tvDns),
        )),
        TvSettingsGroup(R.string.tv_settings_group_3, listOf(
            TvSettingSpec(R.id.llWp, R.string.tv_setting_llwp, R.string.tv_setting_llwp_description),
            TvSettingSpec(R.id.llWpRecovery, R.string.tv_setting_llwprecovery, R.string.tv_setting_llwprecovery_description),
            TvSettingSpec(R.id.llBackup, R.string.tv_setting_llbackup, R.string.tv_setting_llbackup_description),
            TvSettingSpec(R.id.llClearCache, R.string.tv_setting_llclearcache, R.string.tv_setting_llclearcache_description),
            TvSettingSpec(R.id.llAbout, R.string.tv_setting_llabout, R.string.tv_setting_llabout_description),
            TvSettingSpec(R.id.llDebug, R.string.tv_setting_lldebug, R.string.tv_setting_lldebug_description, R.id.tvDebugOpen, visible = false),
        )),
    )
    val rows = groups.flatMap { it.rows }
}
