package com.example.autoanswer.calllog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.autoanswer.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 接听记录列表适配器。
 */
class CallLogAdapter : ListAdapter<CallLogEntry, CallLogAdapter.ViewHolder>(DiffCallback) {

    companion object {
        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    object DiffCallback : DiffUtil.ItemCallback<CallLogEntry>() {
        override fun areItemsTheSame(oldItem: CallLogEntry, newItem: CallLogEntry): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CallLogEntry, newItem: CallLogEntry): Boolean =
            oldItem == newItem
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberText: TextView = itemView.findViewById(R.id.text_call_log_number)
        val timeText: TextView = itemView.findViewById(R.id.text_call_log_time)
        val modeText: TextView = itemView.findViewById(R.id.text_call_log_mode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.numberText.text = entry.number
        holder.timeText.text = TIME_FORMAT.format(Date(entry.timestamp))
        holder.modeText.text = if (entry.mode == "whitelist") {
            holder.itemView.context.getString(R.string.mode_whitelist)
        } else {
            holder.itemView.context.getString(R.string.mode_blacklist)
        }
    }
}
