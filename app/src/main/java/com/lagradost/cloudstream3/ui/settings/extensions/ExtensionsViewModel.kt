package com.lagradost.cloudstream3.ui.settings.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.apmap
import com.lagradost.cloudstream3.mvvm.Some
import com.lagradost.cloudstream3.mvvm.debugAssert
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.plugins.PluginManager.getPluginsOnline
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.plugins.RepositoryManager.PREBUILT_REPOSITORIES
import com.lagradost.cloudstream3.ui.result.UiText
import com.lagradost.cloudstream3.ui.result.txt
import kotlinx.coroutines.launch

data class RepositoryData(
    @JsonProperty("name") val name: String,
    @JsonProperty("url") val url: String
)

const val REPOSITORIES_KEY = "REPOSITORIES_KEY"

class ExtensionsViewModel : ViewModel() {
    data class PluginStats(
        val total: Int,

        val downloaded: Int,
        val disabled: Int,
        val notDownloaded: Int,

        val downloadedText: UiText,
        val disabledText: UiText,
        val notDownloadedText: UiText,
    )

    private val _repositories = MutableLiveData<Array<RepositoryData>>()
    val repositories: LiveData<Array<RepositoryData>> = _repositories

    private val _pluginStats: MutableLiveData<Some<PluginStats>> = MutableLiveData(Some.None)
    val pluginStats: LiveData<Some<PluginStats>> = _pluginStats

    //TODO CACHE GET REQUESTS
    fun loadStats() = viewModelScope.launch {
        val urls = (getKey<Array<RepositoryData>>(REPOSITORIES_KEY)
            ?: emptyArray()) + PREBUILT_REPOSITORIES

        val onlinePlugins = urls.toList().apmap {
            RepositoryManager.getRepoPlugins(it.url)?.toList() ?: emptyList()
        }.flatten().distinctBy { it.second.url }

        // Iterates over all offline plugins, compares to remote repo and returns the plugins which are outdated
        val outdatedPlugins = getPluginsOnline().map { savedData ->
            onlinePlugins.filter { onlineData -> savedData.internalName == onlineData.second.internalName }
                .map { onlineData ->
                    PluginManager.OnlinePluginData(savedData, onlineData)
                }
        }.flatten().distinctBy { it.onlineData.second.url }
        val total = onlinePlugins.count()
        val disabled = outdatedPlugins.count { it.isDisabled }
        val downloadedTotal = outdatedPlugins.count()
        val downloaded = downloadedTotal - disabled
        val notDownloaded = total - downloadedTotal
        val stats = PluginStats(
            total,
            downloaded,
            disabled,
            notDownloaded,
            txt(R.string.plugins_downloaded, downloaded),
            txt(R.string.plugins_disabled, disabled),
            txt(R.string.plugins_not_downloaded, notDownloaded)
        )
        debugAssert({ stats.downloaded + stats.notDownloaded + stats.disabled != stats.total }) {
            "downloaded(${stats.downloaded}) + notDownloaded(${stats.notDownloaded}) + disabled(${stats.disabled}) != total(${stats.total})"
        }
        _pluginStats.postValue(Some.Success(stats))
    }

    private fun repos() = (getKey<Array<RepositoryData>>(REPOSITORIES_KEY)
        ?: emptyArray()) + PREBUILT_REPOSITORIES

    fun loadRepositories() {
        val urls = repos()
        _repositories.postValue(urls)
    }
}