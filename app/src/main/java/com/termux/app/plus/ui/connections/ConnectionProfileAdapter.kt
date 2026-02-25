package com.termux.app.plus.ui.connections

import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.plus.data.model.ConnectionProfile
import com.termux.app.plus.service.ActiveSessionTracker
import com.termux.terminal.TerminalSession

class ConnectionProfileAdapter(
    private val onNewTerminal: (ConnectionProfile) -> Unit,
    private val onResumeSession: (ConnectionProfile, TerminalSession) -> Unit,
    private val onKillSession: (ConnectionProfile, TerminalSession) -> Unit,
    private val onSessionLongClick: (ConnectionProfile, TerminalSession, Int) -> Boolean,
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

    fun refreshSessions() {
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileHeader: View = itemView.findViewById(R.id.profile_header)
        private val nickname: TextView = itemView.findViewById(R.id.connection_nickname)
        private val host: TextView = itemView.findViewById(R.id.connection_host)
        private val sessionCountBadge: TextView = itemView.findViewById(R.id.session_count_badge)
        private val sessionsContainer: LinearLayout = itemView.findViewById(R.id.sessions_container)
        private val newTerminalButton: TextView = itemView.findViewById(R.id.new_terminal_button)

        fun bind(profile: ConnectionProfile) {
            nickname.text = profile.nickname
            host.text = "${profile.username}@${profile.host}:${profile.port}"

            val activeSessions = ActiveSessionTracker.getActiveSessionsForProfile(profile.id)
            val hasActiveSessions = activeSessions.isNotEmpty()

            // Session count badge
            if (hasActiveSessions) {
                sessionCountBadge.text = activeSessions.size.toString()
                sessionCountBadge.visibility = View.VISIBLE
            } else {
                sessionCountBadge.visibility = View.GONE
            }

            // Active sessions list
            sessionsContainer.removeAllViews()
            if (hasActiveSessions) {
                sessionsContainer.visibility = View.VISIBLE
                activeSessions.forEachIndexed { index, session ->
                    sessionsContainer.addView(createSessionItemView(profile, session, index + 1))
                }
            } else {
                sessionsContainer.visibility = View.GONE
            }

            // "New Terminal" link - shown when sessions exist
            // When no sessions exist, tapping the header creates a new terminal
            newTerminalButton.visibility = if (hasActiveSessions) View.VISIBLE else View.GONE
            newTerminalButton.setOnClickListener { onNewTerminal(profile) }

            // Header tap: if no sessions, create new terminal; if sessions exist, also create new
            profileHeader.setOnClickListener {
                if (!hasActiveSessions) {
                    onNewTerminal(profile)
                }
                // When sessions exist, user uses "+ New Terminal" or taps a session
            }

            itemView.setOnLongClickListener { onLongClick(profile) }
        }

        private fun createSessionItemView(
            profile: ConnectionProfile,
            session: TerminalSession,
            index: Int
        ): View {
            val context = itemView.context
            val dp = context.resources.displayMetrics.density

            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
                background = context.getDrawable(android.R.attr.selectableItemBackground.let {
                    val attrs = context.obtainStyledAttributes(intArrayOf(it))
                    val drawable = attrs.getDrawable(0)
                    attrs.recycle()
                    return@let 0
                }.let { android.R.drawable.list_selector_background })
                isClickable = true
                isFocusable = true
                isLongClickable = true
                setOnClickListener { onResumeSession(profile, session) }
                setOnLongClickListener { onSessionLongClick(profile, session, index) }
            }

            // Green dot indicator
            val dot = View(context).apply {
                val size = (8 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setBackgroundResource(R.drawable.badge_background)
            }
            row.addView(dot)

            // Session name
            val label = TextView(context).apply {
                text = session.mSessionName ?: "Terminal $index"
                typeface = Typeface.MONOSPACE
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(label)

            // Kill button
            val killBtn = ImageButton(context).apply {
                val size = (32 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size)
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setBackgroundResource(android.R.drawable.list_selector_background)
                contentDescription = context.getString(R.string.tp_disconnect)
                setOnClickListener { onKillSession(profile, session) }
            }
            row.addView(killBtn)

            return row
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
