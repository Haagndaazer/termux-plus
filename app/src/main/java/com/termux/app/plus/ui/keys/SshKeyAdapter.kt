package com.termux.app.plus.ui.keys

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R

class SshKeyAdapter(
    private val onClick: (SshKeyInfo) -> Unit,
    private val onLongClick: (SshKeyInfo) -> Boolean
) : RecyclerView.Adapter<SshKeyAdapter.ViewHolder>() {

    private var keys: List<SshKeyInfo> = emptyList()

    fun submitList(newKeys: List<SshKeyInfo>) {
        keys = newKeys
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ssh_key, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(keys[position])
    }

    override fun getItemCount() = keys.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val keyName: TextView = itemView.findViewById(R.id.key_name)
        private val keyType: TextView = itemView.findViewById(R.id.key_type)

        fun bind(key: SshKeyInfo) {
            keyName.text = key.name
            keyType.text = key.keyType ?: "unknown"
            itemView.setOnClickListener { onClick(key) }
            itemView.setOnLongClickListener { onLongClick(key) }
        }
    }
}
