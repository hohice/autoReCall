package com.example.autoanswer

import android.app.Application
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import com.example.autoanswer.calllog.CallLogEntry
import com.example.autoanswer.calllog.CallLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 通话控制：根据黑白名单规则处理来电，
 * 符合条件时自动接听、延迟 [hangupDelayMillis] 后挂断，并记录接听日志。
 */
class CallManager(
    private val app: Application,
    private val preferencesRepository: PreferencesRepository,
    private val blacklistRepository: BlacklistRepository,
    private val callLogRepository: CallLogRepository
) {

    companion object {
        private const val TAG = "CallManager"

        /** 接听后自动挂断的延迟时间（毫秒）。修改此值可调整自动挂断时长。 */
        const val hangupDelayMillis = 3000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val telecomManager: TelecomManager
        get() = app.getSystemService(TelecomManager::class.java)

    /**
     * 来电处理入口：检查开关与名单规则，决定是否自动接听。
     */
    fun handleIncomingCall(number: String?) {
        scope.launch {
            if (!preferencesRepository.isEnabled()) {
                Log.d(TAG, "自动接听已关闭，忽略来电")
                return@launch
            }

            val blacklisted = number != null && blacklistRepository.isBlacklisted(number)
            val shouldAnswer = when (preferencesRepository.getMode()) {
                PreferencesRepository.MODE_BLACKLIST -> blacklisted
                PreferencesRepository.MODE_WHITELIST -> !blacklisted
                else -> false
            }

            Log.d(TAG, "来电: $number, 模式: ${preferencesRepository.getMode()}, " +
                "黑名单: $blacklisted, 自动接听: $shouldAnswer")

            if (!shouldAnswer) return@launch

            answerCall()
            callLogRepository.addEntry(
                CallLogEntry(
                    number = number ?: "未知号码",
                    timestamp = System.currentTimeMillis(),
                    mode = preferencesRepository.getMode()
                )
            )
            delay(hangupDelayMillis)
            hangUpCall()
        }
    }

    /**
     * 接听响铃中的电话，需要 ANSWER_PHONE_CALLS 权限。
     * 注意：answerRingingCall() 在 API 35 的 SDK 中已被移除，
     * 这里使用等价的 acceptRingingCall()（自 API 23 起可用，功能相同）。
     */
    private fun answerCall() {
        try {
            telecomManager.acceptRingingCall()
            Log.d(TAG, "已自动接听")
        } catch (e: SecurityException) {
            Log.e(TAG, "接听失败：缺少 ANSWER_PHONE_CALLS 权限", e)
        } catch (e: Exception) {
            Log.e(TAG, "接听失败", e)
        }
    }

    /**
     * 挂断电话：API 28+ 使用 TelecomManager.endCall()；
     * API 26/27 通过反射调用 TelephonyManager 隐藏的 getEndCallMethod。
     */
    private fun hangUpCall() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                telecomManager.endCall()
                Log.d(TAG, "已自动挂断")
            } else {
                endCallByReflection()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "挂断失败：缺少权限", e)
        } catch (e: Exception) {
            Log.e(TAG, "挂断失败", e)
        }
    }

    @Suppress("UNCHECKED_CAST", "PrivateApi")
    private fun endCallByReflection() {
        try {
            val telephonyManager = app.getSystemService(TelephonyManager::class.java)
            val method = telephonyManager.javaClass.getDeclaredMethod("getEndCallMethod")
            method.isAccessible = true
            val iTelephony = method.invoke(telephonyManager)
            iTelephony.javaClass.getMethod("endCall").invoke(iTelephony)
            Log.d(TAG, "已自动挂断（反射）")
        } catch (e: Exception) {
            Log.e(TAG, "反射挂断失败", e)
        }
    }
}
