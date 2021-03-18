package com.dirror.music.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dirror.music.MyApplication
import com.dirror.music.util.Config

/**
 * 搜索 ViewModel
 */
class SearchViewModel: ViewModel() {

    companion object {
        const val ENGINE_NETEASE = 1
        const val ENGINE_QQ = 2
        const val ENGINE_KUWO = 3
    }

    /* 搜索引擎 */
    var searchEngine = MutableLiveData(MyApplication.config.mmkv.decodeInt(Config.SEARCH_ENGINE, ENGINE_NETEASE))

}