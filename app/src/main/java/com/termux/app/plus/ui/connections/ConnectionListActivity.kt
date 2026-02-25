package com.termux.app.plus.ui.connections

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.termux.app.plus.service.SessionLauncher
import com.termux.app.plus.ui.keys.SshKeyListActivity
import com.termux.app.plus.ui.settings.TermuxPlusSettingsActivity
import com.termux.app.plus.ui.snippets.SnippetListActivity
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
            onClick = { profile -> connectToProfile(profile) },
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

    private fun showProfileContextMenu(profile: ConnectionProfile) {
        val options = arrayOf(
            getString(R.string.tp_edit_connection),
            getString(R.string.tp_delete_connection)
        )
        AlertDialog.Builder(this)
            .setTitle(profile.nickname)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, ConnectionEditActivity::class.java)
                        intent.putExtra("profile_id", profile.id)
                        startActivity(intent)
                    }
                    1 -> confirmDeleteProfile(profile)
                }
            }
            .show()
    }

    private fun confirmDeleteProfile(profile: ConnectionProfile) {
        AlertDialog.Builder(this)
            .setTitle(R.string.tp_delete_connection)
            .setMessage(getString(R.string.tp_delete_connection_confirm, profile.nickname))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.deleteProfile(profile)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
