package com.example.autoanswer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限检查和请求：检查全部必需权限，缺失时请求，
 * 被永久拒绝时引导到系统设置页。
 */
class PermissionManager(private val activity: Activity) {

    companion object {
        val REQUIRED_PERMISSIONS: Array<String>
            get() {
                val perms = mutableListOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.ANSWER_PHONE_CALLS,
                    // API 29+ 获取来电号码的前提（否则回调中号码为空串）
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_CONTACTS
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                return perms.toTypedArray()
            }

        fun allPermissionsGranted(activity: Activity): Boolean =
            missingPermissions(activity).isEmpty()

        fun missingPermissions(activity: Activity): Array<String> =
            REQUIRED_PERMISSIONS.filter {
                ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
    }

    /** 返回尚未授予的权限列表 */
    fun missingPermissions(): Array<String> = missingPermissions(activity)

    fun allGranted(): Boolean = missingPermissions().isEmpty()

    /**
     * 请求缺失的权限（供 ActivityResultLauncher 使用）。
     */
    fun shouldShowRationale(): Boolean =
        REQUIRED_PERMISSIONS.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }

    /** 被永久拒绝（或拒绝后不再弹窗）时引导到本应用的系统设置页 */
    fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", activity.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }
}
