package com.aistra.hail.ui.home

import android.app.Activity
import android.os.Bundle
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.HPackages

class HiddenAppProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName == null || !isValidProxy(packageName)) {
            finish()
            return
        }

        HDhizuku.init()
        if (HPackages.isAppHidden(packageName) && !HDhizuku.setAppHidden(packageName, false)) {
            HUI.showToast(com.aistra.hail.R.string.permission_denied)
            finish()
            return
        }

        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        } ?: HUI.showToast(com.aistra.hail.R.string.activity_not_found)
        finish()
    }

    private fun isValidProxy(packageName: String): Boolean =
        HailData.checkedList.any {
            it.packageName == packageName &&
                it.mode == HailData.MODE_DHIZUKU_HIDE &&
                it.applicationInfo != null
        }

    companion object {
        const val EXTRA_PACKAGE = "package"
    }
}
