package com.termux.app.plus.ui.snippets

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.termux.R
import com.termux.app.plus.data.db.TermuxPlusDatabase
import com.termux.app.plus.data.model.Snippet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SnippetListActivity : AppCompatActivity() {

    private val dao by lazy { TermuxPlusDatabase.getInstance(this).snippetDao() }
    private lateinit var adapter: SnippetAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snippet_list)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeSnippets()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.snippets_recycler_view)
        emptyView = findViewById(R.id.empty_view)

        adapter = SnippetAdapter(
            onClick = { snippet -> showEditDialog(snippet) },
            onLongClick = { snippet ->
                confirmDelete(snippet)
                true
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fab_add_snippet).setOnClickListener {
            showEditDialog(null)
        }
    }

    private fun observeSnippets() {
        lifecycleScope.launch {
            dao.getAll().collectLatest { snippets ->
                adapter.submitList(snippets)
                emptyView.visibility = if (snippets.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (snippets.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun showEditDialog(existing: Snippet?) {
        val dialog = SnippetEditDialog()
        dialog.existingSnippet = existing
        dialog.onSave = { name, command, autoExecute ->
            lifecycleScope.launch {
                if (existing != null) {
                    dao.update(existing.copy(name = name, command = command, autoExecute = autoExecute))
                } else {
                    dao.insert(Snippet(name = name, command = command, autoExecute = autoExecute))
                }
                runOnUiThread {
                    Toast.makeText(this@SnippetListActivity, R.string.tp_snippet_saved, Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show(supportFragmentManager, "edit_snippet")
    }

    private fun confirmDelete(snippet: Snippet) {
        AlertDialog.Builder(this)
            .setTitle(R.string.tp_delete_snippet)
            .setMessage(getString(R.string.tp_delete_snippet_confirm, snippet.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch { dao.delete(snippet) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
