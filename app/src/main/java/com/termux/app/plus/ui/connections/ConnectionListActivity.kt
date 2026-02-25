package com.termux.app.plus.ui.connections

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.termux.R
import com.termux.app.TermuxActivity
import com.termux.app.plus.data.model.ConnectionProfile
import com.termux.app.plus.service.ActiveSessionTracker
import com.termux.app.plus.service.SessionLauncher
import com.termux.app.plus.ui.keys.SshKeyListActivity
import com.termux.app.plus.ui.settings.TermuxPlusSettingsActivity
import com.termux.app.plus.ui.snippets.SnippetListActivity
import com.termux.terminal.TerminalSession
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ConnectionListActivity : AppCompatActivity() {

    private lateinit var viewModel: ConnectionListViewModel
    private lateinit var adapter: ConnectionProfileAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_list)

        viewModel = ViewModelProvider(this)[ConnectionListViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeConnections()
    }

    override fun onResume() {
        super.onResume()
        // Refresh session display when returning from terminal
        ActiveSessionTracker.cleanup()
        adapter.refreshSessions()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_local_terminal -> {
                    SessionLauncher.launchLocalTerminal(this)
                    true
                }
                R.id.action_snippets -> {
                    startActivity(Intent(this, SnippetListActivity::class.java))
                    true
                }
                R.id.action_ssh_keys -> {
                    startActivity(Intent(this, SshKeyListActivity::class.java))
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, TermuxPlusSettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.connections_recycler_view)
        emptyView = findViewById(R.id.empty_view)

        adapter = ConnectionProfileAdapter(
            onNewTerminal = { profile -> connectToProfile(profile) },
            onResumeSession = { profile, session -> resumeSession(profile, session) },
            onKillSession = { profile, session -> killSession(profile, session) },
            onSessionLongClick = { profile, session, index ->
                showSessionContextMenu(profile, session, index)
                true
            },
            onLongClick = { profile ->
                showProfileContextMenu(profile)
                true
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_add_connection).setOnClickListener {
            startActivity(Intent(this, ConnectionEditActivity::class.java))
        }
    }

    private fun observeConnections() {
        lifecycleScope.launch {
            viewModel.connections.collectLatest { connections ->
                adapter.submitList(connections)
                emptyView.visibility = if (connections.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (connections.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun connectToProfile(profile: ConnectionProfile) {
        // Check sshpass for password auth
        if (profile.authMethod == "PASSWORD" && !SessionLauncher.isSshpassInstalled()) {
            Toast.makeText(this, R.string.tp_sshpass_not_installed, Toast.LENGTH_LONG).show()
            return
        }

        viewModel.updateLastUsed(profile.id)
        SessionLauncher.launchConnection(profile, this)
    }

    private fun resumeSession(profile: ConnectionProfile, session: TerminalSession) {
        if (!session.isRunning) {
            // Session died, clean up and refresh
            ActiveSessionTracker.cleanup()
            adapter.refreshSessions()
            Toast.makeText(this, R.string.tp_session_ended, Toast.LENGTH_SHORT).show()
            return
        }
        ActiveSessionTracker.pendingResumeSession = session
        val intent = Intent(this, TermuxActivity::class.java).apply {
            putExtra("termux_plus_resume", true)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun killSession(profile: ConnectionProfile, session: TerminalSession) {
        ActiveSessionTracker.killSession(session)
        adapter.refreshSessions()
    }

    private fun showSessionContextMenu(profile: ConnectionProfile, session: TerminalSession, index: Int) {
        val sessionName = session.mSessionName ?: "Terminal $index"
        val options = arrayOf(
            getString(R.string.tp_rename_terminal),
            getString(R.string.tp_disconnect)
        )
        AlertDialog.Builder(this)
            .setTitle(sessionName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> renameSession(session, index)
                    1 -> killSession(profile, session)
                }
            }
            .show()
    }

    private fun renameSession(session: TerminalSession, index: Int) {
        val input = EditText(this).apply {
            setText(session.mSessionName ?: "Terminal $index")
            selectAll()
            setPadding(
                (24 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt(),
                0
            )
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.tp_rename_terminal)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    session.mSessionName = newName
                    adapter.refreshSessions()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showProfileContextMenu(profile: ConnectionProfile) {
        val activeSessions = ActiveSessionTracker.getActiveSessionsForProfile(profile.id)
        val options = mutableListOf(
            getString(R.string.tp_edit_connection),
            getString(R.string.tp_delete_connection)
        )
        if (activeSessions.isNotEmpty()) {
            options.add(getString(R.string.tp_kill_all_sessions))
        }

        AlertDialog.Builder(this)
            .setTitle(profile.nickname)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, ConnectionEditActivity::class.java)
                        intent.putExtra("profile_id", profile.id)
                        startActivity(intent)
                    }
                    1 -> confirmDeleteProfile(profile)
                    2 -> {
                        ActiveSessionTracker.killAllSessionsForProfile(profile.id)
                        adapter.refreshSessions()
                    }
                }
            }
            .show()
    }

    private fun confirmDeleteProfile(profile: ConnectionProfile) {
        // Kill any active sessions first
        val activeSessions = ActiveSessionTracker.getActiveSessionsForProfile(profile.id)
        val message = if (activeSessions.isNotEmpty()) {
            getString(R.string.tp_delete_connection_confirm_active, profile.nickname, activeSessions.size)
        } else {
            getString(R.string.tp_delete_connection_confirm, profile.nickname)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.tp_delete_connection)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ActiveSessionTracker.killAllSessionsForProfile(profile.id)
                viewModel.deleteProfile(profile)
                adapter.refreshSessions()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
