package com.aistra.hail.ui.proxy

import android.app.Activity
import android.os.Bundle
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HPackages

class HiddenAppProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchTarget(intent.getStringExtra(HailData.KEY_PACKAGE))
    }

    private fun launchTarget(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            finish()
            return
        }

        runCatching {
            val pm = packageManager
            HPackages.getApplicationInfoOrNull(packageName)
                ?: throw IllegalStateException("App not found")

            HDhizuku.init()

            if (HPackages.isAppHidden(packageName) && !HDhizuku.setAppHidden(packageName, false)) {
                throw IllegalStateException("Unable to unhide app")
            }

            pm.getLaunchIntentForPackage(packageName)?.let {
                it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
        }

        finish()
    }
}