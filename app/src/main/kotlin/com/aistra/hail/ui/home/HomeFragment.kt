package com.aistra.hail.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData.tags
import com.aistra.hail.databinding.FragmentHomeBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.paddingRelative
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.utils.HShortcuts
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : MainFragment() {
    var multiselect: Boolean = false
    val selectedList: MutableList<AppInfo> = object : ArrayList<AppInfo>() {
        private fun invalidate() {
            if (isAdded) requireActivity().invalidateOptionsMenu()
        }

        override fun add(element: AppInfo): Boolean = super.add(element).also { if (it) invalidate() }
        override fun remove(element: AppInfo): Boolean = super.remove(element).also { if (it) invalidate() }
        override fun clear() {
            super.clear()
            invalidate()
        }
        override fun addAll(elements: Collection<AppInfo>): Boolean =
            super.addAll(elements).also { if (it) invalidate() }
        override fun removeAll(elements: Collection<AppInfo>): Boolean =
            super.removeAll(elements).also { if (it) invalidate() }
    }
    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                menu.add(Menu.NONE, R.id.action_make_proxy, Menu.NONE, R.string.action_make_proxy).apply {
                    setIcon(android.R.drawable.ic_menu_add)
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_make_proxy)?.apply {
                    isVisible = multiselect && selectedList.isNotEmpty()
                    isEnabled = selectedList.any { isProxyEligible(it) }
                }
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                if (item.itemId != R.id.action_make_proxy) return false
                val eligible = selectedList.filter(::isProxyEligible)
                if (eligible.isEmpty()) {
                    Snackbar.make(binding.root, R.string.msg_proxy_none, Snackbar.LENGTH_LONG).show()
                    return true
                }
                eligible.forEach(HShortcuts::addProxyShortcut)
                Snackbar.make(
                    binding.root,
                    R.string.msg_proxy_created,
                    Snackbar.LENGTH_LONG
                ).setText(getString(R.string.msg_proxy_created, eligible.size)).show()
                selectedList.clear()
                multiselect = false
                requireActivity().invalidateOptionsMenu()
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        if (tags.size == 1) binding.tabs.isVisible = false
        binding.pager.adapter = HomeAdapter(this)
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = tags[position].first
        }.attach()
        binding.tabs.applyDefaultInsetter { paddingRelative(isRtl, start = !activity.isLandscape, end = true) }
        return binding.root
    }

    private fun isProxyEligible(info: AppInfo): Boolean =
        info.applicationInfo != null &&
            info.mode == com.aistra.hail.app.HailData.MODE_DHIZUKU_HIDE &&
            com.aistra.hail.utils.HPackages.isAppHidden(info.packageName)

    override fun onDestroyView() {
        multiselect = false
        selectedList.clear()
        super.onDestroyView()
        _binding = null
    }
}