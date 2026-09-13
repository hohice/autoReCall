package com.example.autoanswer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.example.autoanswer.calllog.CallLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * 前台服务（phoneCall 类型）：常驻监听来电状态，
 * 来电时交由 [CallManager] 按黑白名单规则处理。
 */
class CallService : Service() {

    companion object {
        private const val TAG = "CallService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "call_monitor"
        private const val CHANNEL_NAME = "通话监听"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var telephonyManager: TelephonyManager
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    private lateinit var callManager: CallManager

    /** 号码以 PhoneStateListener 回调为准，通话结束后清空防止串号 */
    @Volatile
    private var lastIncomingNumber: String? = null

    override fun onCreate() {
        super.onCreate()
        val app = application
        val database = BlacklistDatabase.getInstance(app)
        callManager = CallManager(
            app,
            PreferencesRepository(app),
            BlacklistRepository(database.blacklistDao()),
            CallLogRepository(database.callLogDao())
        )
        telephonyManager = getSystemService(TelephonyManager::class.java)
        startInForeground()
        registerCallStateListener()
        Log.d(TAG, "CallService 已启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterCallStateListener()
        serviceScope.cancel()
        super.onDestroy()
        Log.d(TAG, "CallService 已停止")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setSmallIcon(R.drawable.ic_call)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    /**
     * 注册来电状态监听。
     * API 31+ 使用 TelephonyCallback（推荐），但由于 TelephonyCallback.CallStateListener
     * 不提供来电号码，同时保留一个 PhoneStateListener 仅用于获取号码；
     * API 26-30 回退到 PhoneStateListener。
     *
     * 两个监听源几乎同时回调，且 TelephonyCallback 通常先到但没有号码，
     * 因此这里延迟短暂时间合并两个来源的事件，号码以 PhoneStateListener 为准。
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun registerCallStateListener() {
        phoneStateListener = object : PhoneStateListener() {
            @Deprecated("API 31+ 推荐 TelephonyCallback，但它是获取来电号码的唯一途径")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING ->
                        onRingingFrom("PhoneStateListener", phoneNumber)
                    else -> lastIncomingNumber = null
                }
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (state == TelephonyManager.CALL_STATE_RINGING) {
                        // TelephonyCallback 不带号码，合并时读取 PhoneStateListener 缓存的号码
                        onRingingFrom("TelephonyCallback", null)
                    }
                }
            }
            telephonyManager.registerTelephonyCallback(mainExecutor, telephonyCallback!!)
        }
    }

    private fun unregisterCallStateListener() {
        phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        phoneStateListener = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
            telephonyCallback = null
        }
    }

    /**
     * 合并两个监听源的响铃事件：延迟短暂时间再处理，
     * 等待 PhoneStateListener 把来电号码写入 [lastIncomingNumber]。
     */
    private val handler = Handler(Looper.getMainLooper())
    private var pendingRinging: Runnable? = null

    private fun onRingingFrom(source: String, number: String?) {
        if (!number.isNullOrBlank()) {
            lastIncomingNumber = number
        }
        pendingRinging?.let { handler.removeCallbacks(it) }
        val task = Runnable {
            val n = lastIncomingNumber?.takeIf { it.isNotBlank() }
            Log.d(TAG, "检测到来电响铃 (来源 $source): $n")
            callManager.handleIncomingCall(n)
        }
        pendingRinging = task
        handler.postDelayed(task, 500)
    }
}
