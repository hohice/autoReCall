package com.example.autoanswer.calllog

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.autoanswer.BlacklistDatabase
import com.example.autoanswer.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 接听记录查看界面。
 */
class CallLogActivity : AppCompatActivity() {

    private val viewModel: CallLogViewModel by lazy {
        ViewModelProvider(this, CallLogViewModel.factory(application))[CallLogViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_call_log)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.call_log_root)) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_call_log)
        val emptyView = findViewById<TextView>(R.id.text_call_log_empty)
        val buttonClear = findViewById<Button>(R.id.button_clear_call_log)

        val adapter = CallLogAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        buttonClear.setOnClickListener {
            viewModel.clear()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entries.collect { list ->
                    adapter.submitList(list)
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}

/**
 * 简单的接听记录 ViewModel。
 */
class CallLogViewModel(
    private val repository: CallLogRepository
) : ViewModel() {

    companion object {
        fun factory(app: android.app.Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = BlacklistDatabase.getInstance(app)
                return CallLogViewModel(CallLogRepository(database.callLogDao())) as T
            }
        }
    }

    private val _entries = MutableStateFlow<List<CallLogEntry>>(emptyList())
    val entries: StateFlow<List<CallLogEntry>> = _entries.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAll().collect { _entries.value = it }
        }
    }

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }
}
