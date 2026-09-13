package com.example.autoanswer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.autoanswer.calllog.CallLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 设置与状态管理：总开关、模式（黑名单/白名单）、黑名单号码列表。
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val database = BlacklistDatabase.getInstance(app)
    private val preferencesRepository = PreferencesRepository(app)
    private val blacklistRepository = BlacklistRepository(database.blacklistDao())
    private val callLogRepository = CallLogRepository(database.callLogDao())

    private val _enabled = MutableStateFlow(preferencesRepository.isEnabled())
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _mode = MutableStateFlow(preferencesRepository.getMode())
    val mode: StateFlow<String> = _mode.asStateFlow()

    val blacklist: StateFlow<List<BlacklistEntry>> =
        blacklistRepository.getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setEnabled(enabled: Boolean) {
        preferencesRepository.setEnabled(enabled)
        _enabled.value = enabled
    }

    fun setMode(mode: String) {
        preferencesRepository.setMode(mode)
        _mode.value = mode
    }

    fun addBlacklistNumber(number: String, note: String? = null) {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            blacklistRepository.addNumber(trimmed, note)
        }
    }

    fun deleteBlacklistEntry(entry: BlacklistEntry) {
        viewModelScope.launch {
            blacklistRepository.delete(entry)
        }
    }
}
