package com.aistra.hail.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.*

class SettingsFragment : MainFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    ProvidePreferenceLocals { SettingsScreen() }
                }
            }
        }

    @Composable
    private fun SettingsScreen() {
        val autoFreezeAfterLock = rememberPreferenceState(HailData.AUTO_FREEZE_AFTER_LOCK, false)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            listPreference(
                key = HailData.WORKING_MODE,
                defaultValue = HailData.MODE_DHIZUKU_HIDE,
                onValueChange = ::onWorkingModeChange,
                values = HailData.WORKING_MODE_VALUES,
                entriesId = R.array.working_mode_entries,
                titleId = R.string.working_mode,
                icon = Icons.Outlined.Adb,
                type = ListPreferenceType.ALERT_DIALOG
            )
            switchPreference(
                key = HailData.BIOMETRIC_LOGIN,
                defaultValue = false,
                onValueChange = { _, value ->
                    if (value) resetDynamicShortcuts()
                    true
                },
                titleId = R.string.action_biometric,
                icon = Icons.Outlined.Fingerprint
            )
            horizontalDivider()
            preferenceCategory(key = "customize", title = { Text(text = stringResource(R.string.title_customize)) })
            listPreference(
                key = HailData.APP_THEME,
                defaultValue = HailData.FOLLOW_SYSTEM,
                onValueChange = { _, value ->
                    app.setAppTheme(value)
                    true
                },
                values = HailData.APP_THEME_VALUES,
                entriesId = R.array.app_theme_entries,
                titleId = R.string.app_theme,
                icon = Icons.Outlined.DarkMode
            )
            listPreference(
                key = HailData.ICON_PACK,
                defaultValue = HailData.ACTION_NONE,
                onValueChange = { _, _ ->
                    AppIconCache.clear()
                    true
                },
                values = mutableListOf(HailData.ACTION_NONE).apply {
                    addAll(Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME").let {
                        if (HTarget.T) app.packageManager.queryIntentActivities(
                            it, PackageManager.ResolveInfoFlags.of(0)
                        ) else app.packageManager.queryIntentActivities(it, 0)
                    }.map { it.activityInfo.packageName })
                },
                titleId = R.string.icon_pack,
                icon = Icons.Outlined.Palette,
                summary = { iconPackName(it) },
                valueToText = ::iconPackName
            )
            switchPreference(HailData.GRAYSCALE_ICON, true, titleId = R.string.grayscale_icon, icon = Icons.Outlined.FilterBAndW)
            switchPreference(HailData.COMPACT_ICON, false, titleId = R.string.compact_icon, icon = Icons.Outlined.Apps)
            switchPreference(HailData.SYNTHESIZE_ADAPTIVE_ICONS, false, titleId = R.string.synthesize_adaptive_icons, icon = Icons.Outlined.Layers)
            sliderPreference(
                key = HailData.HOME_FONT_SIZE,
                defaultValue = 14f,
                title = { Text(text = stringResource(R.string.home_font_size)) },
                valueRange = 11f..16f,
                valueSteps = 4,
                icon = { Icon(imageVector = Icons.Outlined.TextFields, contentDescription = null) },
                valueText = { Text(text = "%.0f".format(it)) },
            )
            switchPreference(HailData.FUZZY_SEARCH, false, titleId = R.string.fuzzy_search, icon = Icons.AutoMirrored.Outlined.ManageSearch)
            switchPreference(HailData.NINE_KEY_SEARCH, false, titleId = R.string.nine_key, icon = Icons.Outlined.Dialpad)
            listPreference(
                key = HailData.TILE_ACTION,
                defaultValue = HailData.tileAction,
                values = HailData.TILE_ACTION_VALUES,
                entriesId = R.array.tile_action_entries,
                titleId = R.string.tile_action,
                icon = Icons.Outlined.DashboardCustomize
            )
            horizontalDivider()
            preferenceCategory(key = "auto_freeze", title = { Text(text = stringResource(R.string.auto_freeze)) })
            switchPreference(
                rememberState = { autoFreezeAfterLock },
                onValueChange = { _, value ->
                    app.setAutoFreezeService(value)
                    true
                },
                titleId = R.string.auto_freeze_after_lock,
                icon = Icons.Outlined.ScreenLockPortrait
            )
            sliderPreference(
                key = HailData.AUTO_FREEZE_DELAY,
                defaultValue = 0f,
                title = { Text(text = stringResource(R.string.auto_freeze_delay)) },
                valueRange = 0f..30f,
                valueSteps = 29,
                enabled = { autoFreezeAfterLock.value },
                icon = { Icon(imageVector = Icons.Outlined.LockClock, contentDescription = null) },
                valueText = { Text(text = "%.0f".format(it)) },
            )
            switchPreference(HailData.SKIP_WHILE_CHARGING, false, titleId = R.string.skip_while_charging, enabled = autoFreezeAfterLock.value, icon = Icons.Outlined.BatteryChargingFull)
            switchPreference(
                key = HailData.SKIP_FOREGROUND_APP,
                defaultValue = false,
                onValueChange = { _, value ->
                    if (value && !HSystem.checkOpUsageStats(requireContext())) {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        false
                    } else true
                },
                titleId = R.string.skip_foreground_app,
                enabled = autoFreezeAfterLock.value,
                icon = Icons.Outlined.Android
            )
            switchPreference(
                key = HailData.SKIP_NOTIFYING_APP,
                defaultValue = false,
                onValueChange = { _, value ->
                    val isGranted = NotificationManagerCompat.getEnabledListenerPackages(requireContext())
                        .contains(requireContext().packageName)
                    if (value && !isGranted) {
                        app.setAutoFreezeServiceEnabled(true)
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        false
                    } else true
                },
                titleId = R.string.skip_notifying_app,
                enabled = autoFreezeAfterLock.value,
                icon = Icons.Outlined.NotificationsActive
            )
            horizontalDivider()
            preferenceCategory(key = "shortcuts", title = { Text(text = stringResource(R.string.title_shortcuts)) })
            preference(
                key = "add_pin_shortcut",
                title = { Text(text = stringResource(R.string.action_add_pin_shortcut)) },
                icon = { Icon(imageVector = Icons.AutoMirrored.Outlined.Shortcut, contentDescription = null) },
                onClick = ::addPinShortcut
            )
            listPreference(
                key = HailData.DYNAMIC_SHORTCUT_ACTION,
                defaultValue = HailData.ACTION_NONE,
                onValueChange = { _, action ->
                    HShortcuts.removeAllDynamicShortcuts()
                    HShortcuts.addDynamicShortcutAction(action)
                    true
                },
                values = HailData.DYNAMIC_SHORTCUT_ACTIONS,
                entriesId = R.array.dynamic_shortcut_entries,
                titleId = R.string.dynamic_shortcut_action,
                icon = Icons.Outlined.AppShortcut
            )
            preference(
                key = "clear_dynamic_shortcuts",
                title = { Text(text = stringResource(R.string.action_clear_dynamic_shortcuts)) },
                icon = { Icon(imageVector = Icons.Outlined.CleaningServices, contentDescription = null) },
                onClick = ::resetDynamicShortcuts
            )
        }
    }

    private fun LazyListScope.horizontalDivider() = item { HorizontalDivider() }

    private fun LazyListScope.switchPreference(
        rememberState: @Composable () -> MutableState<Boolean>,
        onValueChange: (MutableState<Boolean>, Boolean) -> Boolean = { _, _ -> true },
        @StringRes titleId: Int,
        enabled: Boolean = true,
        icon: ImageVector,
    ) = item(key = titleId, contentType = "SwitchPreference") {
        val state = rememberState()
        SwitchPreference(
            value = state.value,
            onValueChange = { if (onValueChange(state, it)) state.value = it },
            title = { Text(text = stringResource(titleId)) },
            enabled = enabled,
            icon = { Icon(imageVector = icon, contentDescription = null) })
    }

    private fun LazyListScope.switchPreference(
        key: String,
        defaultValue: Boolean,
        onValueChange: (MutableState<Boolean>, Boolean) -> Boolean = { _, _ -> true },
        @StringRes titleId: Int,
        enabled: Boolean = true,
        icon: ImageVector,
    ) = switchPreference(
        rememberState = { rememberPreferenceState(key, defaultValue) },
        onValueChange = onValueChange,
        titleId = titleId,
        enabled = enabled,
        icon = icon
    )

    private fun LazyListScope.listPreference(
        key: String,
        defaultValue: String,
        onValueChange: (MutableState<String>, String) -> Boolean = { _, _ -> true },
        values: List<String>,
        @StringRes titleId: Int,
        icon: ImageVector,
        summary: @Composable (String) -> String,
        type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
        valueToText: (String) -> String
    ) = item(key = key, contentType = "ListPreference") {
        val state = rememberPreferenceState(key, defaultValue)
        ListPreference(
            value = state.value,
            onValueChange = { if (onValueChange(state, it)) state.value = it },
            values = values,
            title = { Text(text = stringResource(titleId)) },
            icon = { Icon(imageVector = icon, contentDescription = null) },
            summary = { Text(text = summary(state.value)) },
            type = type,
            valueToText = { AnnotatedString(valueToText(it)) })
    }

    private fun LazyListScope.listPreference(
        key: String,
        defaultValue: String,
        onValueChange: (MutableState<String>, String) -> Boolean = { _, _ -> true },
        values: List<String>,
        @ArrayRes entriesId: Int,
        @StringRes titleId: Int,
        icon: ImageVector,
        type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU
    ) = listPreference(
        key = key,
        defaultValue = defaultValue,
        onValueChange = onValueChange,
        values = values,
        titleId = titleId,
        icon = icon,
        summary = { it.toEntry(values, entriesId) },
        type = type,
        valueToText = { it.toEntry(values, entriesId) })

    private fun String.toEntry(values: List<String>, @ArrayRes entriesId: Int): String =
        resources.getStringArray(entriesId)[values.indexOf(this)]

    private fun resetDynamicShortcuts() {
        HShortcuts.removeAllDynamicShortcuts()
        HShortcuts.addDynamicShortcutAction(HailData.dynamicShortcutAction)
    }

    private fun iconPackName(pack: String): String = if (pack == HailData.ACTION_NONE) getString(R.string.action_none)
    else HPackages.getApplicationInfoOrNull(pack)?.loadLabel(app.packageManager)?.toString() ?: pack

    private fun addPinShortcut() {
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_add_pin_shortcut)
            .setItems(R.array.pin_shortcut_entries) { _, which ->
                when (which) {
                    0 -> MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_freeze_tag)
                        .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                            val tag = HailData.tags[index].first
                            HShortcuts.addPinShortcut(
                                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_round_frozen_shortcut)!!,
                                HailApi.ACTION_FREEZE_TAG + tag,
                                tag,
                                HailApi.getIntentForTag(HailApi.ACTION_FREEZE_TAG, tag)
                            )
                        }.setNegativeButton(android.R.string.cancel, null).show()

                    1 -> MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_unfreeze_tag)
                        .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                            val tag = HailData.tags[index].first
                            HShortcuts.addPinShortcut(
                                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_round_unfrozen_shortcut)!!,
                                HailApi.ACTION_UNFREEZE_TAG + tag,
                                tag,
                                HailApi.getIntentForTag(HailApi.ACTION_UNFREEZE_TAG, tag)
                            )
                        }.setNegativeButton(android.R.string.cancel, null).show()

                    2 -> HShortcuts.addPinShortcut(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_round_frozen_shortcut)!!, HailApi.ACTION_FREEZE_ALL, getString(R.string.action_freeze_all), Intent(HailApi.ACTION_FREEZE_ALL))
                    3 -> HShortcuts.addPinShortcut(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_round_unfrozen_shortcut)!!, HailApi.ACTION_UNFREEZE_ALL, getString(R.string.action_unfreeze_all), Intent(HailApi.ACTION_UNFREEZE_ALL))
                    4 -> HShortcuts.addPinShortcut(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_round_frozen_shortcut)!!, HailApi.ACTION_FREEZE_NON_WHITELISTED, getString(R.string.action_freeze_non_whitelisted), Intent(HailApi.ACTION_FREEZE_NON_WHITELISTED))
                    5 -> HShortcuts.addPinShortcut(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_outline_lock_shortcut)!!, HailApi.ACTION_LOCK, getString(R.string.action_lock), Intent(HailApi.ACTION_LOCK))
                    6 -> HShortcuts.addPinShortcut(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_outline_lock_shortcut)!!, HailApi.ACTION_LOCK_FREEZE, getString(R.string.action_lock_freeze), Intent(HailApi.ACTION_LOCK_FREEZE))
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun onWorkingModeChange(rememberState: MutableState<String>, mode: String): Boolean = runCatching {
        Dhizuku.init(app)
        when {
            Dhizuku.isPermissionGranted() -> true
            else -> {
                lifecycleScope.launch {
                    val result = callbackFlow {
                        Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                            override fun onRequestPermission(grantResult: Int) {
                                trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                            }
                        })
                        awaitClose()
                    }.first()
                    if (result) {
                        rememberState.value = mode
                        if (HTarget.O) HDhizuku.setDelegatedScopes()
                    }
                }
                false
            }
        }
    }.getOrElse {
        HLog.e(it)
        HUI.showToast(R.string.permission_denied)
        false
    }
}