package com.aistra.hail.ui.home

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ActionProvider
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.aistra.hail.R
import com.aistra.hail.utils.HShortcuts
import com.google.android.material.snackbar.Snackbar

class ProxyActionProvider(context: Context) : ActionProvider(context) {
    override fun onCreateActionView(): View = ImageButton(context).apply {
        background = ColorDrawable(android.graphics.Color.TRANSPARENT)
        setImageResource(R.drawable.ic_outline_select_all)
        imageTintList = ContextCompat.getColorStateList(context, android.R.color.white)
        contentDescription = context.getString(R.string.action_make_proxy)
        setOnClickListener { makeProxy() }
    }

    override fun onPerformDefaultAction(): Boolean {
        makeProxy()
        return true
    }

    private fun makeProxy() {
        val activity = context as? FragmentActivity ?: return
        val home = (activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)
            ?.childFragmentManager?.fragments?.filterIsInstance<HomeFragment>()?.firstOrNull()
        val selected = home?.selectedList ?: emptyList()
        if (selected.isEmpty()) {
            Snackbar.make(activity.findViewById(R.id.toolbar), R.string.tap_to_select, Snackbar.LENGTH_SHORT).show()
            return
        }
        val count = selected.count { HShortcuts.addProxyShortcut(it) }
        Snackbar.make(
            activity.findViewById(R.id.toolbar),
            if (count == 0) R.string.permission_denied else "Created proxy: $count",
            Snackbar.LENGTH_SHORT
        ).show()
    }
}
