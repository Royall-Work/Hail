package com.aistra.hail.ui.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainActivity
import com.aistra.hail.utils.HPolicy
import com.aistra.hail.utils.HUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView

class SettingsFragment : PreferenceFragmentCompat(), MenuProvider {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MenuHost).addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showTerminalDialog() {
        // Existing implementation
    }

    private fun ownerRemoveDialog() {
        (requireActivity() as MainActivity).ownerRemoveDialog()
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_terminal -> showTerminalDialog()
            R.id.action_remove_owner -> ownerRemoveDialog()
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        if (HailData.workingMode.startsWith(HailData.SU) || HailData.workingMode.startsWith(
                HailData.SHIZUKU
            )
        ) {
            menu.findItem(R.id.action_remove_owner)?.isVisible = false
        }
    }
}