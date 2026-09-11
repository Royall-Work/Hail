package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.aistra.hail.HailApp.Companion.app

object HPackages {
    val myUserId get() = android.os.Process.myUserHandle().hashCode()

    fun packageUri(packageName: String) = "package:$packageName"

    fun getInstalledApplications(flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192): List<ApplicationInfo> =
        if (HTarget.T) app.packageManager.getInstalledApplications(
            PackageManager.ApplicationInfoFlags.of(flags.toLong())
        ) else app.packageManager.getInstalledApplications(flags)

    fun getUnhiddenPackageInfoOrNull(
        packageName: String, flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192
    ) = runCatching {
        if (HTarget.T) app.packageManager.getPackageInfo(
            packageName, PackageManager.PackageInfoFlags.of(flags.toLong())
        ) else app.packageManager.getPackageInfo(packageName, flags)
    }.getOrNull()

    fun getApplicationInfoOrNull(
        packageName: String, flags: Int = if (HTarget.N) PackageManager.MATCH_UNINSTALLED_PACKAGES else 8192
    ) = runCatching {
        if (HTarget.T) app.packageManager.getApplicationInfo(
            packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong())
        ) else app.packageManager.getApplicationInfo(packageName, flags)
    }.getOrNull()

    fun isAppHidden(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let {
        (ApplicationInfo::class.java.getField("privateFlags").get(it) as Int) and 1 == 1
    } ?: false

    fun isAppSuspended(packageName: String): Boolean = getApplicationInfoOrNull(packageName)?.let {
        when {
            HTarget.N -> it.flags and ApplicationInfo.FLAG_SUSPENDED == ApplicationInfo.FLAG_SUSPENDED
            else -> false
        }
    } ?: false

    fun isAppUninstalled(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.run {
            flags and ApplicationInfo.FLAG_INSTALLED != ApplicationInfo.FLAG_INSTALLED
        } ?: true

    fun canUninstallNormally(packageName: String): Boolean =
        getApplicationInfoOrNull(packageName)?.sourceDir?.startsWith("/data") ?: false

    fun getApplicationLabel(packageName: String) =
        getApplicationInfoOrNull(packageName)?.loadLabel(app.packageManager)
}