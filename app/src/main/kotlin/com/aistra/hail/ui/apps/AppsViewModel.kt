package com.aistra.hail.ui.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.*
import kotlinx.coroutines.*

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    val apps = MutableLiveData<List<ApplicationInfo>>()
    val isRefreshing = MutableLiveData(false)
    val query = MutableLiveData("")
    val displayApps = MutableLiveData<List<ApplicationInfo>>()

    init {
        updateAppList()
    }

    private var refreshJob: Job? = null
    private var refreshStateJob: Job? = null

    private fun postRefreshState(state: Boolean, delayTime: Long = 200L) {
        if (!state) {
            refreshStateJob?.cancel()
            isRefreshing.postValue(false)
        } else if (refreshStateJob == null || refreshStateJob!!.isCompleted) {
            refreshStateJob = viewModelScope.launch {
                delay(delayTime)
                isRefreshing.postValue(true)
            }
        }
    }

    fun postQuery(text: String, delayTime: Long = 300L) {
        refreshJob?.cancel()
        if (delayTime == 0L)
            query.postValue(text)
        else {
            refreshJob = viewModelScope.launch {
                delay(delayTime)
                query.postValue(text)
            }
        }
    }

    fun updateAppList() {
        viewModelScope.launch {
            postRefreshState(true)
            apps.postValue(HPackages.getInstalledApplications())
        }
    }

    fun updateDisplayAppList() {
        apps.value?.let {
            viewModelScope.launch {
                postRefreshState(true)
                displayApps.postValue(filterList(it, query.value))
                postRefreshState(false)
            }
        }
    }

    private val ApplicationInfo.isSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
    private val ApplicationInfo.isAppUninstalled: Boolean
        get() = HPackages.isAppUninstalled(packageName)
    private val ApplicationInfo.isAppFrozen get() = AppManager.isAppFrozen(packageName)

    private suspend fun filterList(
        appList: List<ApplicationInfo>,
        query: String?
    ): List<ApplicationInfo> {
        val pm = getApplication<HailApp>().packageManager
        return withContext(Dispatchers.Default) {
            return@withContext appList.filter {
                val isUninstalled = it.isAppUninstalled
                val typeMatches =
                    (HailData.filterUserApps && !it.isSystemApp && !isUninstalled)
                            || (HailData.filterSystemApps && it.isSystemApp && !isUninstalled)
                            || (HailData.filterUninstalledApps && isUninstalled)

                typeMatches
                        && ((HailData.filterFrozenApps && it.isAppFrozen)
                        || (HailData.filterUnfrozenApps && !it.isAppFrozen))
                        && ((HailData.nineKeySearch
                        && (NineKeySearch.search(query, it.packageName, it.loadLabel(pm).toString())))
                        || FuzzySearch.search(it.packageName, query)
                        || FuzzySearch.search(it.loadLabel(pm).toString(), query)
                        || PinyinSearch.searchPinyinAll(it.loadLabel(pm).toString(), query))
            }.run {
                when (HailData.sortBy) {
                    HailData.SORT_INSTALL -> sortedBy {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)
                            ?.firstInstallTime ?: 0
                    }

                    HailData.SORT_UPDATE -> sortedByDescending {
                        HPackages.getUnhiddenPackageInfoOrNull(it.packageName)?.lastUpdateTime ?: 0
                    }

                    else -> sortedWith(NameComparator)
                }
            }
        }
    }
}