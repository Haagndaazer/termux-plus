package com.termux.app.plus.ui.connections

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.plus.data.model.ConnectionProfile

class ConnectionProfileAdapter(
    private val onClick: (ConnectionProfile) -> Unit,
    private val onLongClick: (ConnectionProfile) -> Boolean
) : ListAdapter<ConnectionProfile, ConnectionProfileAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connection_profile, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val profile = getItem(position)
        holder.bind(profile)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nickname: TextView = itemView.findViewById(R.id.connection_nickname)
        private val host: TextView = itemView.findViewById(R.id.connection_host)
        private val lastUsed: TextView = itemView.findViewById(R.id.connection_last_used)

        fun bind(profile: ConnectionProfile) {
            nickname.text = profile.nickname
            host.text = "${profile.username}@${profile.host}:${profile.port}"
            lastUsed.text = if (profile.lastUsedAt != null) {
                itemView.context.getString(
                    R.string.tp_last_used,
                    DateUtils.getRelativeTimeSpanString(
                        profile.lastUsedAt,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    )
                )
            } else {
                itemView.context.getString(R.string.tp_never_used)
            }

            itemView.setOnClickListener { onClick(profile) }
            itemView.setOnLongClickListener { onLongClick(profile) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ConnectionProfile>() {
            override fun areItemsTheSame(old: ConnectionProfile, new: ConnectionProfile) =
                old.id == new.id

            override fun areContentsTheSame(old: ConnectionProfile, new: ConnectionProfile) =
                old == new
        }
    }
}
