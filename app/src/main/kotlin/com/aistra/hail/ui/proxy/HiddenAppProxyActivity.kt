package com.aistra.hail.ui.proxy

import android.app.Activity
import android.os.Bundle
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HDhizuku
import com.aistra.hail.utils.HPackages

class HiddenAppProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(HailData.KEY_PACKAGE)
        if (packageName.isNullOrBlank() || HPackages.getApplicationInfoOrNull(packageName) == null) {
            finish()
            return
        }

        runCatching {
            HDhizuku.init()
            if (HPackages.isAppHidden(packageName)) {
                check(HDhizuku.setAppHidden(packageName, false))
            }
            packageManager.getLaunchIntentForPackage(packageName)?.let(::startActivity)
        }
        finish()
    }
}
