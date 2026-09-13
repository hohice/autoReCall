package com.example.autoanswer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 偏好设置存储：保存总开关状态和黑白名单模式。
 * 使用 SharedPreferences，轻量且无需额外依赖。
 */
class PreferencesRepository(context: Context) {

    companion object {
        const val MODE_BLACKLIST = "blacklist" // 仅黑名单号码自动接听
        const val MODE_WHITELIST = "whitelist" // 非黑名单号码自动接听

        private const val PREFS_NAME = "auto_answer_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_MODE = "mode"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
    }

    /** 当前模式，默认黑名单模式 */
    fun getMode(): String = prefs.getString(KEY_MODE, MODE_BLACKLIST) ?: MODE_BLACKLIST

    fun setMode(mode: String) {
        prefs.edit { putString(KEY_MODE, mode) }
    }
}
