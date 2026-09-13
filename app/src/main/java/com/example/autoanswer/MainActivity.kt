package com.example.autoanswer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.autoanswer.calllog.CallLogActivity
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

/**
 * 主界面：权限检查、监听开关、模式切换、黑名单管理。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var viewModel: SettingsViewModel
    private lateinit var adapter: BlacklistAdapter

    private var showingPermissionDialog = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { !it }) {
                // 有权限被拒绝：若不再弹窗（永久拒绝），引导到系统设置
                if (!permissionManager.shouldShowRationale()) {
                    showGoToSettingsDialog()
                } else {
                    showPermissionRequiredDialog()
                }
            } else {
                updateServiceSwitchState()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scroll_root)) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        permissionManager = PermissionManager(this)
        viewModel = androidx.lifecycle.ViewModelProvider(this)[SettingsViewModel::class.java]

        val switchMonitor = findViewById<MaterialSwitch>(R.id.switch_monitor)
        val radioBlacklist = findViewById<RadioButton>(R.id.radio_blacklist)
        val radioWhitelist = findViewById<RadioButton>(R.id.radio_whitelist)
        val editNumber = findViewById<EditText>(R.id.edit_blacklist_number)
        val buttonAdd = findViewById<Button>(R.id.button_add_blacklist)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_blacklist)
        val buttonCallLog = findViewById<Button>(R.id.button_open_call_log)

        adapter = BlacklistAdapter { entry ->
            viewModel.deleteBlacklistEntry(entry)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 监听开关：启动 / 停止前台服务
        switchMonitor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !permissionManager.allGranted()) {
                switchMonitor.isChecked = false
                showPermissionRequiredDialog()
                return@setOnCheckedChangeListener
            }
            viewModel.setEnabled(isChecked)
            if (isChecked) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, CallService::class.java)
                )
                Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show()
            } else {
                stopService(Intent(this, CallService::class.java))
                Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show()
            }
        }

        // 模式切换
        radioBlacklist.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setMode(PreferencesRepository.MODE_BLACKLIST)
        }
        radioWhitelist.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.setMode(PreferencesRepository.MODE_WHITELIST)
        }

        // 添加黑名单号码
        buttonAdd.setOnClickListener {
            val number = editNumber.text.toString().trim()
            if (number.isEmpty()) {
                Toast.makeText(this, R.string.number_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addBlacklistNumber(number)
            editNumber.text.clear()
        }

        // 跳转接听记录页
        buttonCallLog.setOnClickListener {
            startActivity(Intent(this, CallLogActivity::class.java))
        }

        // 观察 ViewModel 状态
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.enabled.collect { switchMonitor.isChecked = it }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mode.collect { mode ->
                    radioBlacklist.isChecked = mode == PreferencesRepository.MODE_BLACKLIST
                    radioWhitelist.isChecked = mode == PreferencesRepository.MODE_WHITELIST
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.blacklist.collect { list -> adapter.submitList(list) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceSwitchState()
        if (!permissionManager.allGranted() && !showingPermissionDialog) {
            showPermissionRequiredDialog()
        }
    }

    private fun updateServiceSwitchState() {
        // 若权限缺失（例如用户在设置页撤销了权限），同步开关状态
        if (!permissionManager.allGranted()) {
            viewModel.setEnabled(false)
        }
    }

    /** 缺失权限时显示对话框引导授权 */
    private fun showPermissionRequiredDialog() {
        if (showingPermissionDialog) return
        showingPermissionDialog = true
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_required_message)
            .setCancelable(false)
            .setPositiveButton(R.string.permission_grant) { _, _ ->
                showingPermissionDialog = false
                val missing = permissionManager.missingPermissions()
                if (missing.isNotEmpty()) {
                    permissionLauncher.launch(missing)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                showingPermissionDialog = false
                Toast.makeText(this, R.string.permission_denied_hint, Toast.LENGTH_LONG).show()
            }
            .setOnDismissListener { showingPermissionDialog = false }
            .show()
    }

    /** 权限被永久拒绝时引导到系统设置页 */
    private fun showGoToSettingsDialog() {
        if (showingPermissionDialog) return
        showingPermissionDialog = true
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_permanently_denied_message)
            .setCancelable(false)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                showingPermissionDialog = false
                permissionManager.openAppSettings()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> showingPermissionDialog = false }
            .setOnDismissListener { showingPermissionDialog = false }
            .show()
    }
}
