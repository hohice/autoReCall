package com.example.autoanswer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * 黑名单号码列表适配器。
 */
class BlacklistAdapter(
    private val onDeleteClick: (BlacklistEntry) -> Unit
) : ListAdapter<BlacklistEntry, BlacklistAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<BlacklistEntry>() {
        override fun areItemsTheSame(oldItem: BlacklistEntry, newItem: BlacklistEntry): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: BlacklistEntry, newItem: BlacklistEntry): Boolean =
            oldItem == newItem
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberText: TextView = itemView.findViewById(R.id.text_blacklist_number)
        val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete_blacklist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blacklist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = getItem(position)
        holder.numberText.text = entry.number
        holder.deleteButton.setOnClickListener { onDeleteClick(entry) }
    }
}
