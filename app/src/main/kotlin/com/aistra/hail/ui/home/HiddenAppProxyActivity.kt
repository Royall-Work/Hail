package com.aistra.hail.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HUI

class HiddenAppProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(EXTRA_PACKAGE)
        if (packageName.isNullOrEmpty() || !HailData.isChecked(packageName)) {
            finish()
            return
        }

        if (HailData.getAppMode(packageName) != HailData.MODE_DHIZUKU_HIDE ||
            !AppManager.isAppFrozen(packageName)
        ) {
            finish()
            return
        }

        if (!AppManager.setAppFrozen(packageName, false)) {
            HUI.showToast(R.string.permission_denied)
            finish()
            return
        }

        val launchIntent = app.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            AppManager.setAppFrozen(packageName, true)
            HUI.showToast(R.string.activity_not_found)
            finish()
            return
        }

        runCatching {
            startActivity(launchIntent)
        }.onFailure {
            AppManager.setAppFrozen(packageName, true)
            HUI.showToast(R.string.operation_failed, it.message ?: "")
        }
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "package"
    }
}