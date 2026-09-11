package com.aistra.hail.app

import android.content.Intent
import com.aistra.hail.BuildConfig
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HUI

object AppManager {
    val lockScreen: Boolean
        get() = HDhizuku.lockScreen

    private fun getWorkingMode(packageName: String): String = HailData.getAppMode(packageName)

    fun isAppFrozen(packageName: String): Boolean = when (getWorkingMode(packageName)) {
        HailData.MODE_DHIZUKU_HIDE -> HPackages.isAppHidden(packageName)
        HailData.MODE_DHIZUKU_SUSPEND -> HPackages.isAppSuspended(packageName)
        else -> false
    }

    fun setListFrozen(frozen: Boolean, vararg appInfo: AppInfo): String? {
        val excludeMe = appInfo.filter { it.packageName != BuildConfig.APPLICATION_ID }
        var i = 0
        var denied = false
        var name = String()
        excludeMe.forEach {
            when {
                setAppFrozen(it.packageName, frozen) -> {
                    i++
                    name = it.name.toString()
                }

                it.applicationInfo != null -> denied = true
            }
        }
        return if (denied && i == 0) null else if (i == 1) name else i.toString()
    }

    fun setAppFrozen(packageName: String, frozen: Boolean): Boolean =
        packageName != BuildConfig.APPLICATION_ID && when (getWorkingMode(packageName)) {
            HailData.MODE_DHIZUKU_HIDE -> HDhizuku.setAppHidden(packageName, frozen)
            HailData.MODE_DHIZUKU_SUSPEND -> HDhizuku.setAppSuspended(packageName, frozen)
            else -> false
        }

    fun uninstallApp(packageName: String): Boolean =
        if (HDhizuku.uninstallApp(packageName)) true
        else {
            HUI.startActivity(Intent.ACTION_DELETE, HPackages.packageUri(packageName))
            false
        }
}